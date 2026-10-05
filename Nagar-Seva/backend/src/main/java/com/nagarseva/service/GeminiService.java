package com.nagarseva.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nagarseva.entity.Complaint;
import com.nagarseva.entity.ComplaintPriority;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-3.5-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private CloseableHttpClient httpClient = HttpClients.createDefault();

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getModel() {
        return this.model;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String executeWithFallback(ObjectNode requestBody) throws Exception {
        List<String> modelsToTry = getCandidateModels();
        Exception lastException = null;
        for (String candidateModel : modelsToTry) {
            String url = GEMINI_BASE_URL + candidateModel + ":generateContent?key=" + apiKey;
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(requestBody), ContentType.APPLICATION_JSON));
            try (var response = httpClient.execute(httpPost)) {
                int status = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), java.nio.charset.StandardCharsets.UTF_8);
                if (status == 200) {
                    return responseBody;
                }
                log.warn("Gemini model {} returned status {}. Attempting fallback...", candidateModel, status);
                lastException = new RuntimeException("Model " + candidateModel + " failed with status " + status + ": " + responseBody);
            } catch (Exception ex) {
                log.warn("Gemini model {} failed with exception: {}. Attempting fallback...", candidateModel, ex.getMessage());
                lastException = ex;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new RuntimeException("No Gemini models available");
    }

    private List<String> getCandidateModels() {
        List<String> models = new java.util.ArrayList<>();
        if (model != null && !model.isBlank()) {
            models.add(model.trim());
        }
        List<String> fallbacks = List.of("gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-3.8-flash", "gemini-flash-latest");
        for (String fb : fallbacks) {
            if (!models.contains(fb)) {
                models.add(fb);
            }
        }
        return models;
    }


    public record ComplaintAnalysisResult(
            String routedAuthority,
            String aiSummary,
            ComplaintPriority priority,
            Boolean imageVerified,
            String imageVerificationNote
    ) {}

    public record ResolutionVerificationResult(
            boolean resolutionVerified,
            String resolutionVerificationNote
    ) {}

    public record PhotoVerificationResult(
            boolean verified,
            String detectedContent,
            String explanation,
            String suggestedCategory
    ) {}

    /**
     * Analyze complaint text and image using Google Gemini
     */
    public ComplaintAnalysisResult classifyAndVerifyComplaint(Complaint complaint, String photoData) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY not configured - using fallback routing");
            return fallbackClassification(complaint, photoData);
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            ArrayNode contentsArray = requestBody.putArray("contents");
            ObjectNode contentObj = contentsArray.addObject();
            ArrayNode partsArray = contentObj.putArray("parts");

            boolean hasPhoto = photoData != null && !photoData.isBlank();
            String photoInstruction = hasPhoto
                    ? """
                      Photo Verification Rules (Citizen-Centric & Real-World Grounded):
                      - The citizen has attached a photo as visual proof of the civic issue.
                      - REAL-WORLD CITIZEN CONTEXT: Citizens are regular everyday residents capturing photos with basic mobile phones. Photos are frequently taken at night, dusk, or in challenging low-light conditions, from a distance, or with motion blur / camera shake.
                      - Streetlight / Lighting: Streetlights are mounted 15-25 feet high. Photos taken from the ground or at night will naturally show dark street corridors, lamp post silhouettes, unlit bulb fixtures, utility poles, or overhead wires. DO NOT reject a streetlight photo for being dark, blurry, distant, or grainy! If the image shows an outdoor street, lamppost, utility pole, electrical wire, or dark road stretch, you MUST set "imageVerified": true.
                      - Road Damage, Drainage, Illegal Dumping, Unsafe Area, Encroachment: Any real outdoor scene showing roads, pavement, asphalt defects, puddles, gutters, drains, trash piles, or public spaces MUST be accepted ("imageVerified": true).
                      - BENEFIT OF THE DOUBT: If an image appears to be an authentic photo taken outdoors in a real neighborhood or public space related to the complaint, ALWAYS set "imageVerified": true.
                      - STRICT REJECTION (ONLY FOR FAKE/UNRELATED CONTENT): ONLY set "imageVerified": false if the image is clearly and undeniably fake, synthetic, or non-civic:
                        * Video game cover art, gameplay screenshots, video games
                        * Internet memes, cartoons, anime, digital illustrations, computer wallpapers
                        * Selfies or personal portraits of people posing
                        * Indoor residential rooms (living room, bedroom, bathroom, kitchen, domestic furniture)
                        * Digital screenshots of software dashboards, web pages, or text documents
                        * Food plates, domestic pets/house animals
                      """
                    : "No photo was attached. Set \"imageVerified\": null and \"imageVerificationNote\": null.";

            String prompt = String.format("""
                    You are NagarSeva AI, an intelligent civic governance and multimodal computer vision assistant.
                    Analyze the following civic complaint details submitted by a citizen:
                    Category: %s
                    Description: %s
                    Location: %s
                    Ward: %s
                    
                    %s
                    
                    Respond strictly in valid JSON format with NO markdown wrapping:
                    {
                      "routedAuthority": "Exact municipal department responsible (e.g., Public Works Department (PWD), Electricity Board, Jal Sansthan, Sanitation & Waste Management, Traffic Police)",
                      "aiSummary": "A concise 1-2 sentence executive summary of the issue",
                      "priority": "LOW or MEDIUM or HIGH",
                      "imageVerified": true or false or null,
                      "imageVerificationNote": "Detailed visual evaluation and reason for approval or rejection"
                    }
                    """,
                    complaint.getCategory(),
                    complaint.getDescription(),
                    complaint.getLocation(),
                    complaint.getWard(),
                    photoInstruction
            );

            partsArray.addObject().put("text", prompt);

            if (hasPhoto) {
                attachInlineImage(partsArray, photoData);
            }

            String responseBody = executeWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractTextFromGeminiResponse(root);
            JsonNode json = parseCleanJson(text);

            String routedAuthority = json.path("routedAuthority").asText("General Municipal Administration");
            String aiSummary = json.path("aiSummary").asText(complaint.getDescription());
            String priorityStr = json.path("priority").asText("MEDIUM").toUpperCase();
            ComplaintPriority priority = switch (priorityStr) {
                case "HIGH" -> ComplaintPriority.HIGH;
                case "LOW" -> ComplaintPriority.LOW;
                default -> ComplaintPriority.MEDIUM;
            };

            Boolean imageVerified = null;
            String imageVerificationNote = null;
            if (hasPhoto) {
                if (json.has("imageVerified") && !json.get("imageVerified").isNull()) {
                    imageVerified = json.get("imageVerified").asBoolean(false);
                } else {
                    imageVerified = false;
                }
                imageVerificationNote = json.path("imageVerificationNote").asText(
                        Boolean.TRUE.equals(imageVerified)
                                ? "Verified: Image matches reported civic issue."
                                : "Image does not match reported civic defect."
                );
            }

            return new ComplaintAnalysisResult(routedAuthority, aiSummary, priority, imageVerified, imageVerificationNote);
        } catch (Exception e) {
            log.error("Gemini analysis error: {}", e.getMessage(), e);
        }

        return fallbackClassification(complaint, photoData);
    }

    /**
     * Dedicated multimodal AI vision verification for citizen grievance photos.
     * Evaluates whether the uploaded photo actually depicts the reported category and issue,
     * specifically rejecting video game covers, memes, selfies, indoor scenes, or fake evidence.
     */
    public PhotoVerificationResult verifyGrievancePhoto(String category, String description, String photoData) {
        if (photoData == null || photoData.isBlank()) {
            return new PhotoVerificationResult(false, "No photo provided", "Please provide a photo for verification.", null);
        }

        if (apiKey == null || apiKey.isBlank()) {
            return new PhotoVerificationResult(
                    true,
                    "Photo attached (AI offline)",
                    "AI image verification is offline. Photo flagged for manual officer inspection.",
                    null
            );
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            ArrayNode contentsArray = requestBody.putArray("contents");
            ObjectNode contentObj = contentsArray.addObject();
            ArrayNode partsArray = contentObj.putArray("parts");

            String prompt = String.format("""
                    You are NagarSeva AI, an empathetic municipal civic auditor and computer vision inspector.
                    A citizen is reporting a civic grievance and has attached a photo as visual evidence.
                    Reported Category: %s
                    Reported Description: %s
                    
                    CRITICAL REAL-WORLD CITIZEN PHOTOGRAPHY GUIDELINES:
                    The citizens using this application are regular everyday residents (not professional photographers). They capture photos using mobile phones:
                    1. Photos are frequently taken in difficult real-world conditions: at night, dusk, in rain, or while moving.
                    2. Photos may be dark, grainy, blurry, shaky, taken from across the street, or cropped awkwardly.
                    3. Specific Guidance:
                       - Streetlight / Lighting: Streetlights are mounted 15-25 feet high. At night, citizen photos show a dark street, dark sky, silhouette of a lamp post, utility pole, wiring, or unlit fixture. During the day, photos show an outdoor street, lamp head, broken/shattered glass globe, exposed bulb socket, utility bracket, or pole against the sky or road. NEVER reject a streetlight photo because "the bulb is broken", "it is taken from below", or "the sky is bright"! If there is a streetlight pole, shattered globe, lamppost, lamp fixture, bulb, wiring, or dark street corridor in the image, it is VALID civic evidence (verified: true).
                       - Road Damage: Any pavement, asphalt, road craters, cracks, water puddles on road, or sidewalk damage (even if taken from a car or bike) MUST be accepted (verified: true).
                       - Drainage: Murky water, puddles, gutters, manholes, flooded curbs, or drains MUST be accepted (verified: true).
                       - Illegal Dumping: Trash piles, garbage bags, litter, overflowing dumpsters, or roadside debris MUST be accepted (verified: true).
                       - Unsafe Area: Dark alleys, unlit roads, isolated pathways, broken boundary walls MUST be accepted (verified: true).
                       - Encroachment: Stalls, carts, parked vehicles or obstacles on footpaths/roads MUST be accepted (verified: true).
                    4. CROSS-CATEGORY CIVIC DEFECTS: If the photo depicts ANY genuine municipal defect (e.g. broken streetlight, shattered lamp globe, pothole, overflowing garbage, drainage leak) but the citizen's reported category is slightly different, general, or empty:
                       - You MUST set "verified": true!
                       - Provide the accurate category in "suggestedCategory" (e.g. 'Streetlight', 'Road Damage', 'Drainage', 'Illegal Dumping', 'Unsafe Area', 'Encroachment').
                       - NEVER reject genuine municipal infrastructure damage just because the citizen didn't select the exact right category!
                    5. BENEFIT OF THE DOUBT: If an image appears to be an authentic photo taken outdoors in a real neighborhood or public civic environment related to any civic defect, ALWAYS mark "verified": true.
                    
                    WHAT TO REJECT ("verified": false):
                    You must ONLY reject photos that are clearly, undeniably synthetic, fraudulent, or unrelated to outdoor civic spaces:
                    1. Video game cover art, gameplay screenshots, video games
                    2. Memes, cartoons, anime, digital art, computer wallpapers
                    3. Selfies or portraits of people posing
                    4. Indoor residential rooms (living room, bedroom, bathroom, kitchen, interior furniture)
                    5. Screenshots of software dashboards, web pages, or digital text
                    6. Scanned paper documents, bills, receipts, book pages
                    7. Food plates, domestic pets/house animals
                    
                    Respond strictly in valid JSON format with NO markdown wrapping:
                    {
                      "verified": true or false,
                      "detectedContent": "Concise, respectful description of what is visible in the photo",
                      "explanation": "Empathetic explanation acknowledging real-world citizen photography conditions and confirming acceptance or reason for rejection",
                      "suggestedCategory": "If civic issue but wrong category, suggest the correct one (e.g. 'Drainage'), otherwise null"
                    }
                    """,
                    category != null && !category.isBlank() ? category : "General Civic Issue",
                    description != null && !description.isBlank() ? description : "Civic grievance"
            );

            partsArray.addObject().put("text", prompt);
            attachInlineImage(partsArray, photoData);

            String responseBody = executeWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractTextFromGeminiResponse(root);
            JsonNode json = parseCleanJson(text);

            boolean verified = json.path("verified").asBoolean(false);
            String detectedContent = json.path("detectedContent").asText("Visual content evaluated");
            String explanation = json.path("explanation").asText(
                    verified ? "Photo confirms reported civic issue." : "Photo does not match reported issue."
            );
            String suggestedCategory = json.has("suggestedCategory") && !json.get("suggestedCategory").isNull()
                    ? json.get("suggestedCategory").asText(null)
                    : null;

            return new PhotoVerificationResult(verified, detectedContent, explanation, suggestedCategory);
        } catch (Exception e) {
            log.error("Error verifying grievance photo with Gemini: {}", e.getMessage(), e);
            return new PhotoVerificationResult(
                    true,
                    "Photo attached (Pending Officer Audit)",
                    "Automated AI image verification is temporarily busy. Your photo was attached successfully and flagged for manual field officer inspection.",
                    null
            );
        }
    }

    public ComplaintAnalysisResult analyzeComplaint(Complaint complaint, String photoData) {
        return classifyAndVerifyComplaint(complaint, photoData);
    }

    /**
     * Verify resolution photos (area reference + grievance photo + resolution proof) against category and note.
     */
    public ResolutionVerificationResult verifyResolutionProof(
            String category,
            String resolutionNote,
            String beforePhoto,
            String afterPhoto,
            String areaReferencePhoto) {

        if (apiKey == null || apiKey.isBlank()) {
            return new ResolutionVerificationResult(false, "Manual review required: AI verification unconfigured. Resolution proof flagged for officer audit.");
        }

        try {
            String url = GEMINI_BASE_URL + model + ":generateContent?key=" + apiKey;
            ObjectNode requestBody = objectMapper.createObjectNode();

            ArrayNode contentsArray = requestBody.putArray("contents");
            ObjectNode contentObj = contentsArray.addObject();
            ArrayNode partsArray = contentObj.putArray("parts");

            boolean hasAreaRef = areaReferencePhoto != null && !areaReferencePhoto.isBlank();
            boolean hasBefore = beforePhoto != null && !beforePhoto.isBlank();
            boolean hasAfter = afterPhoto != null && !afterPhoto.isBlank();

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are NagarSeva AI auditor, verifying municipal civic issue resolutions.\n");
            promptBuilder.append(String.format("A municipal authority has marked a '%s' grievance as RESOLVED.\n", category != null ? category : "Civic Issue"));
            promptBuilder.append(String.format("Officer Resolution Note: %s\n\n", resolutionNote != null ? resolutionNote : "Issue marked as resolved"));
            promptBuilder.append("Attached visual evidence (in order):\n");

            int imageIndex = 1;
            if (hasAreaRef) {
                promptBuilder.append(String.format("- Image %d: AREA_REFERENCE (historical reference baseline of this location before any reported grievance; loose context only)\n", imageIndex++));
            }
            if (hasBefore) {
                promptBuilder.append(String.format("- Image %d: GRIEVANCE_PHOTO (the citizen's reported problem showing the civic defect)\n", imageIndex++));
            }
            if (hasAfter) {
                promptBuilder.append(String.format("- Image %d: RESOLUTION_PHOTO (the officer's remediation proof showing the completed repair)\n", imageIndex++));
            }

            promptBuilder.append("\nVerification Instructions:\n");
            promptBuilder.append("1. Treat AREA_REFERENCE as loose, best-effort context only (it may be stale, from a different angle, or missing). Do NOT require an exact match with the area reference.\n");
            promptBuilder.append("2. Base your verdict primarily on comparing GRIEVANCE_PHOTO against RESOLUTION_PHOTO to judge whether the specific defect reported by the citizen has been genuinely repaired and resolved.\n");
            promptBuilder.append("3. In your verification note, explicitly mention whether the area reference was available/useful, and summarize visual evidence supporting your decision.\n\n");
            promptBuilder.append("Respond strictly in valid JSON format with NO markdown wrapping:\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"resolutionVerified\": true or false,\n");
            promptBuilder.append("  \"resolutionVerificationNote\": \"Detailed evaluation explaining if the reported defect was resolved, noting whether area reference was available/useful\"\n");
            promptBuilder.append("}\n");

            partsArray.addObject().put("text", promptBuilder.toString());

            if (hasAreaRef) {
                attachInlineImage(partsArray, areaReferencePhoto);
            }
            if (hasBefore) {
                attachInlineImage(partsArray, beforePhoto);
            }
            if (hasAfter) {
                attachInlineImage(partsArray, afterPhoto);
            }

            String responseBody = executeWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractTextFromGeminiResponse(root);
            JsonNode json = parseCleanJson(text);

            boolean verified = json.path("resolutionVerified").asBoolean(false);
            String note = json.path("resolutionVerificationNote").asText(
                    verified ? "Verified: Resolution photo confirms the reported issue was fixed."
                             : "Manual review required: Resolution photo does not adequately confirm the fix."
            );
            return new ResolutionVerificationResult(verified, note);
        } catch (Exception e) {
            log.error("Resolution verification failed: {}", e.getMessage(), e);
        }

        // Fail-closed fallback: flag as review required rather than silently auto-verifying
        return new ResolutionVerificationResult(false, "Manual review required: Automated AI verification could not confirm resolution proof.");
    }

    public ResolutionVerificationResult verifyResolutionProof(String category, String resolutionNote, String beforePhoto, String afterPhoto) {
        return verifyResolutionProof(category, resolutionNote, beforePhoto, afterPhoto, null);
    }

    public ResolutionVerificationResult verifyResolution(String category, String resolutionNote, String beforePhoto, String afterPhoto) {
        return verifyResolutionProof(category, resolutionNote, beforePhoto, afterPhoto, null);
    }


    /**
     * Interactive Civic Assistant Chatbot response with strict multiturn validation
     */
    public String chatAssistant(String userMessage, List<Map<String, String>> history) {
        if (apiKey == null || apiKey.isBlank()) {
            return generateFallbackChatResponse(userMessage);
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            // System instruction strictly commanding concise, direct answers without repeated boilerplate or reasoning
            ObjectNode systemInstruction = requestBody.putObject("system_instruction");
            systemInstruction.putArray("parts").addObject().put("text", """
                    You are NagarSeva Civic AI Assistant, a direct, concise, and helpful municipal assistant for city residents.

                    CRITICAL INSTRUCTIONS:
                    1. Answer the citizen's specific question DIRECTLY and CONCISELY. Get straight to the point.
                    2. NEVER append a repetitive greeting, welcome intro, capability menu, or platform summary (do NOT say 'Welcome to NagarSeva', do NOT list categories, wards, commands like /track, or dashboard features unless the user specifically asked for them).
                    3. DO NOT include any reasoning, thought process, internal monologue, or meta-commentary in your response.
                    4. Keep answers brief (1 to 2 short paragraphs or clean bullet points). Avoid fluff, boilerplate, or repetitive marketing speech.
                    5. If the user asks for help drafting a complaint, provide only the clear draft title, category, department, and a 2-sentence description.
                    """);

            // Generation config with temperature 0.4 and thinkingBudget 0 to disable thinking/reasoning output
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 0.4);
            generationConfig.put("maxOutputTokens", 800);
            ObjectNode thinkingConfig = generationConfig.putObject("thinkingConfig");
            thinkingConfig.put("thinkingBudget", 0);

            ArrayNode contentsArray = requestBody.putArray("contents");

            // Build clean alternating conversation history ensuring:
            // 1. First turn is always "user"
            // 2. Turns strictly alternate: user -> model -> user -> model
            // 3. Final turn is always the current user message
            List<Map<String, String>> validHistory = new java.util.ArrayList<>();
            if (history != null) {
                boolean foundFirstUser = false;
                String lastRole = null;
                for (Map<String, String> msg : history) {
                    if (msg == null) continue;
                    String role = "user".equalsIgnoreCase(msg.get("role")) ? "user" : "model";
                    String text = msg.get("content");
                    if (text == null || text.isBlank()) continue;

                    // Skip leading model greeting until first user message
                    if (!foundFirstUser) {
                        if ("user".equals(role)) {
                            foundFirstUser = true;
                        } else {
                            continue;
                        }
                    }

                    // Skip duplicate consecutive roles
                    if (role.equals(lastRole)) {
                        continue;
                    }

                    validHistory.add(Map.of("role", role, "content", text.trim()));
                    lastRole = role;
                }
            }

            // If history ends with a user turn, remove it so current userMessage becomes the single final user turn
            if (!validHistory.isEmpty() && "user".equals(validHistory.get(validHistory.size() - 1).get("role"))) {
                validHistory.remove(validHistory.size() - 1);
            }

            for (Map<String, String> msg : validHistory) {
                ObjectNode node = contentsArray.addObject();
                node.put("role", msg.get("role"));
                node.putArray("parts").addObject().put("text", msg.get("content"));
            }

            // Append current user message
            ObjectNode userNode = contentsArray.addObject();
            userNode.put("role", "user");
            userNode.putArray("parts").addObject().put("text", userMessage != null ? userMessage.trim() : "");

            String responseBody = executeWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractTextFromGeminiResponse(root);
            if (!text.isBlank()) {
                return text;
            }
        } catch (Exception e) {
            log.error("Gemini chat error: {}", e.getMessage());
        }

        return generateFallbackChatResponse(userMessage);
    }

    private void attachInlineImage(ArrayNode partsArray, String photoData) {
        if (photoData == null || photoData.isBlank()) {
            return;
        }

        String mimeType = "image/jpeg";
        String base64Data = photoData.trim();

        if (base64Data.startsWith("data:")) {
            int semicolon = base64Data.indexOf(';');
            int comma = base64Data.indexOf(',');
            if (semicolon > 5 && comma > semicolon) {
                mimeType = base64Data.substring(5, semicolon);
                base64Data = base64Data.substring(comma + 1);
            }
        } else if (base64Data.startsWith("/demo-assets/") || base64Data.startsWith("demo-assets/")
                || base64Data.startsWith("area-reference/") || base64Data.startsWith("grievance/") || base64Data.startsWith("resolved/")) {
            String cleanPath = base64Data;
            if (cleanPath.startsWith("/demo-assets/")) {
                cleanPath = "demo-assets/" + cleanPath.substring("/demo-assets/".length());
            } else if (!cleanPath.startsWith("demo-assets/")) {
                cleanPath = "demo-assets/" + cleanPath;
            }
            try {
                org.springframework.core.io.ClassPathResource cpr = new org.springframework.core.io.ClassPathResource(cleanPath);
                if (cpr.exists()) {
                    byte[] bytes = cpr.getInputStream().readAllBytes();
                    base64Data = java.util.Base64.getEncoder().encodeToString(bytes);
                    if (cleanPath.endsWith(".png")) {
                        mimeType = "image/png";
                    }
                }
            } catch (Exception e) {
                log.warn("Could not read demo asset bytes from {}: {}", cleanPath, e.getMessage());
            }
        }

        ObjectNode inlineDataWrapper = partsArray.addObject();
        ObjectNode inlineData = inlineDataWrapper.putObject("inline_data");
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", base64Data);
    }


    private String extractTextFromGeminiResponse(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode part : parts) {
                    // CRITICAL: Ignore thought/reasoning parts so internal thinking is NEVER leaked to the user
                    if (part.path("thought").asBoolean(false)) {
                        continue;
                    }
                    if (part.has("text")) {
                        sb.append(part.path("text").asText());
                    }
                }
                return sb.toString().trim();
            }
        }
        return "";
    }

    private JsonNode parseCleanJson(String rawText) {
        try {
            String clean = rawText.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim();
            return objectMapper.readTree(clean);
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response as JSON: {}", rawText);
            return objectMapper.createObjectNode();
        }
    }

    private ComplaintAnalysisResult fallbackClassification(Complaint complaint, String photoData) {
        String category = complaint.getCategory() != null ? complaint.getCategory() : "Other";
        String routedAuthority;
        ComplaintPriority priority;

        switch (category) {
            case "Streetlight" -> {
                routedAuthority = "Electricity Department";
                priority = ComplaintPriority.MEDIUM;
            }
            case "Drainage" -> {
                routedAuthority = "Jal Sansthan / Drainage Dept";
                priority = ComplaintPriority.HIGH;
            }
            case "Road Damage" -> {
                routedAuthority = "Municipal Road Department";
                priority = ComplaintPriority.HIGH;
            }
            case "Illegal Dumping" -> {
                routedAuthority = "Sanitation & Waste Management";
                priority = ComplaintPriority.MEDIUM;
            }
            case "Unsafe Area" -> {
                routedAuthority = "Police & Municipal Security Cell";
                priority = ComplaintPriority.HIGH;
            }
            default -> {
                routedAuthority = "General Municipal Administration";
                priority = ComplaintPriority.LOW;
            }
        }

        String summary = (complaint.getDescription() != null && complaint.getDescription().length() > 20)
                ? complaint.getDescription()
                : "Civic issue reported in " + complaint.getWard() + " under category " + category;

        boolean hasPhoto = photoData != null && !photoData.isBlank();
        Boolean imageVerified = hasPhoto ? true : null;
        String note = hasPhoto
                ? "Photo attached (Offline mode: Pending municipal officer visual audit)."
                : null;

        return new ComplaintAnalysisResult(routedAuthority, summary, priority, imageVerified, note);
    }

    private String generateFallbackChatResponse(String query) {
        String lower = query.toLowerCase().trim();

        // 1. Complaint Limits & Quotas
        if (lower.contains("how many") || lower.contains("limit") || lower.contains("quota") || lower.contains("can a person") || lower.contains("can i report")) {
            return "📋 **Complaint Limits on NagarSeva:**\n\n" +
                    "- **Unlimited Submissions:** There is **no limit** on how many complaints a citizen can report! You can submit as many civic grievances as you encounter.\n" +
                    "- **Categories:** You can file across `Road Damage`, `Streetlight`, `Drainage`, `Illegal Dumping`, and `Unsafe Area`.\n" +
                    "- **Tracking:** Each ticket receives a unique Tracking ID and appears in **[My Complaints](/my-complaints)**.\n" +
                    "- **Ward Coverage:** We support Ward 1, Ward 2, and Ward 3 with real-time municipal routing.";
        }

        // 2. Image & Vision Verification Queries
        if (lower.contains("verify") || lower.contains("image") || lower.contains("photo") || lower.contains("picture") || lower.contains("vision")) {
            return "📸 **AI Image & Vision Verification:**\n\n" +
                    "**Yes, NagarSeva has built-in AI Image Verification powered by Gemini Vision!**\n\n" +
                    "1. **Citizen Submission Check:** When you upload a photo with your grievance, the AI inspects the image to confirm the defect (e.g. verifying an asphalt pothole, garbage overflow, or broken light fixture).\n" +
                    "2. **Mandatory Resolution Proof:** Municipal officers **cannot** close a ticket without uploading an *'After-Fix Photo Proof'*.\n" +
                    "3. **AI Resolution Audit:** The AI verifies that the after photo actually demonstrates the fix before the complaint is officially marked `RESOLVED`.\n\n" +
                    "💡 *You can attach photos directly on the **[Report Issue](/report)** page!*";
        }

        // 3. Cost / Fees / Charges
        if (lower.contains("cost") || lower.contains("fee") || lower.contains("charge") || lower.contains("free") || lower.contains("price")) {
            return "🆓 **NagarSeva is 100% Free:**\n\n" +
                    "- **Zero Citizen Fees:** Reporting complaints, tracking status, checking municipal resolution proof, and using the AI assistant is completely free.\n" +
                    "- **Zero Map Charges:** Safe Road Navigation and hazard heatmaps use open routing with zero search fees.";
        }

        // 4. Ward & Zonal Jurisdictions
        if (lower.contains("ward") || lower.contains("zone") || lower.contains("jurisdiction")) {
            return "🏛️ **Ward Structure & Coverage:**\n\n" +
                    "- **Ward 1 (Civil Lines & North Zone):** Handled by North Zonal Municipal Cell.\n" +
                    "- **Ward 2 (Rajiv Chowk & Central Zone):** Handled by Central Commercial Zone.\n" +
                    "- **Ward 3 (Lajpat Nagar & South Zone):** Handled by South Residential Sanitation Cell.\n\n" +
                    "You can view ward resolution leaderboards and efficiency on the **[Public Dashboard](/dashboard)**!";
        }

        // 5. Pothole & Road Damage Queries
        if (lower.contains("pothole") || lower.contains("road") || lower.contains("asphalt") || lower.contains("pavement") || lower.contains("crater")) {
            return "🛣️ **Draft Complaint: Road Damage & Pothole**\n\n" +
                    "Here is a recommended format to submit on the **[Report Issue](/report)** page:\n\n" +
                    "- **Category:** `Road Damage`\n" +
                    "- **Priority:** `HIGH`\n" +
                    "- **Suggested Title:** Dangerous pothole causing traffic slowdown & hazard\n" +
                    "- **Description:** *\"A large, hazardous pothole on the main carriageway needs urgent asphalt resurfacing to prevent accidents.\"*\n" +
                    "- **Routed Department:** `PWD / Road Maintenance Department`\n\n" +
                    "💡 *Tip: Attach a clear daytime photo of the damaged section to enable AI auto-verification!*";
        }

        // 6. Streetlight & Dark Spot Queries
        if (lower.contains("streetlight") || lower.contains("light") || lower.contains("dark") || lower.contains("lamp") || lower.contains("unlit") || lower.contains("pole")) {
            return "💡 **Draft Complaint: Streetlight Outage**\n\n" +
                    "Here is a recommended format to submit on the **[Report Issue](/report)** page:\n\n" +
                    "- **Category:** `Streetlight`\n" +
                    "- **Priority:** `MEDIUM`\n" +
                    "- **Suggested Title:** Streetlight non-functional, creating dark zone\n" +
                    "- **Description:** *\"The streetlights along this stretch have been completely unlit for multiple nights, causing pedestrian safety hazards.\"*\n" +
                    "- **Routed Department:** `Electricity Department / Urban Lighting Cell`\n\n" +
                    "🛡️ *Note: Unlit streetlight reports immediately mark safety risk areas on our live **[Safety Map](/safety)**!*";
        }

        // 7. Garbage, Solid Waste & Dumping Queries
        if (lower.contains("garbage") || lower.contains("dump") || lower.contains("trash") || lower.contains("waste") || lower.contains("litter") || lower.contains("sanitation")) {
            return "🗑️ **Draft Complaint: Illegal Garbage Dumping**\n\n" +
                    "Here is a recommended format to submit on the **[Report Issue](/report)** page:\n\n" +
                    "- **Category:** `Illegal Dumping`\n" +
                    "- **Priority:** `MEDIUM`\n" +
                    "- **Suggested Title:** Unattended solid waste overflow\n" +
                    "- **Description:** *\"Accumulated garbage and solid waste on the roadside creating foul smell and unhygienic conditions. Immediate clearing and sanitization required.\"*\n" +
                    "- **Routed Department:** `Sanitation & Waste Management Department`";
        }

        // 8. Drainage, Water Logging & Sewage Queries
        if (lower.contains("drain") || lower.contains("water") || lower.contains("sewer") || lower.contains("leak") || lower.contains("flood") || lower.contains("manhole")) {
            return "🚰 **Draft Complaint: Drainage / Water Logging Issue**\n\n" +
                    "Here is a recommended format to submit on the **[Report Issue](/report)** page:\n\n" +
                    "- **Category:** `Drainage`\n" +
                    "- **Priority:** `HIGH`\n" +
                    "- **Suggested Title:** Blocked municipal drain causing water accumulation\n" +
                    "- **Description:** *\"Heavy blockage in the drainage line resulting in stagnant water overflow. Risk of mosquito breeding and structural road damage.\"*\n" +
                    "- **Routed Department:** `Jal Sansthan & Water Works Authority`";
        }

        // 9. Safety, Crime & Unsafe Areas
        if (lower.contains("safe") || lower.contains("crime") || lower.contains("unsafe") || lower.contains("harass") || lower.contains("security") || lower.contains("patrol")) {
            return "🛡️ **Safety Alert & Area Flagging**\n\n" +
                    "You can flag vulnerable spots to the Municipal Authorities & Local Patrols:\n\n" +
                    "1. Report with category **'Unsafe Area'** on the **[Report Issue](/report)** page.\n" +
                    "2. Check the **[Safety Map](/safety)** to view real-time risk heatmaps calculated from unlit streetlights and active grievances.\n" +
                    "3. Use the **Safe Route Navigator** to compute well-lit, lower-risk travel routes.";
        }

        // 10. Escalation & SLA Queries
        if (lower.contains("escalat") || lower.contains("sla") || lower.contains("delay") || lower.contains("time") || lower.contains("hour") || lower.contains("minute")) {
            return "⏳ **NagarSeva Automated Escalation System**\n\n" +
                    "- **Demo SLA Window:** Issues unresolved after **5 minutes** (representing standard 48-hour municipal SLA) are automatically marked as **`ESCALATED`**.\n" +
                    "- **Executive Alert:** Escalated tickets are highlighted directly on the **[Admin Portal](/admin)** and elevated to Zonal Officers.\n" +
                    "- **Citizen Tracking:** Citizens receive visual status badges in **[My Complaints](/my-complaints)** showing escalation urgency.";
        }

        // 11. Tracking & Status Queries
        if (lower.contains("track") || lower.contains("status") || lower.contains("progress") || lower.contains("check")) {
            return "🔍 **How to Track Your Grievances:**\n\n" +
                    "- **Personal Dashboard:** Visit **[My Complaints](/my-complaints)** to inspect all tickets filed by your account.\n" +
                    "- **Public Registry:** Visit **[Track All Complaints](/track)** to view real-time civic issues across all wards.\n" +
                    "- **Status Lifecycle:** `OPEN` ➡️ `IN_PROGRESS` ➡️ `RESOLVED` (with mandatory photographic evidence).";
        }

        // 12. Admin & Municipal Resolution Queries
        if (lower.contains("admin") || lower.contains("resolve") || lower.contains("officer") || lower.contains("authority") || lower.contains("mayor")) {
            return "🏢 **Municipal Resolution & Verification Standards:**\n\n" +
                    "- **Mandatory Photo Proof:** Field workers must upload an **'After Resolution' photo** before any ticket can be closed.\n" +
                    "- **AI Verification:** The AI system cross-references the before and after photos to confirm the defect has truly been fixed.\n" +
                    "- **Transparency:** Citizens can inspect the repair proof directly on their ticket card.";
        }

        // 13. AI Status & Gemini Model Queries
        if (lower.contains("gemini") || lower.contains("boilerplate") || lower.contains("model") || lower.contains("key") || lower.contains("offline")) {
            return "🤖 **NagarSeva AI Engine Status**\n\n" +
                    "- **Model Target:** `gemini-1.5-flash`\n" +
                    "- **Current Status:** " + (isConfigured() ? "Connected to Google Gemini 1.5 API." : "Offline Fallback Mode (GEMINI_API_KEY is not configured or live API was unreachable).") + "\n\n" +
                    (isConfigured()
                            ? "Your requests are being processed by Google Gemini AI."
                            : "To activate live Gemini conversational intelligence:\n1. Obtain a free key at **[Google AI Studio](https://aistudio.google.com/)**.\n2. Add `GEMINI_API_KEY=your_key` to `.env` in the project root or set `$env:GEMINI_API_KEY=\"your_key\"`.\n3. Restart the backend server.");
        }

        // 14. Greeting / General Queries
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.length() < 5) {
            return "👋 **Hello! I am your NagarSeva Civic AI Assistant.**\n\nHow can I help you today? Feel free to describe a civic issue you've noticed or ask a specific municipal question.";
        }

        // 15. Default Smart Conversational Response
        return "I can assist with reporting civic issues, tracking ticket progress, or checking safety routes across the city. Feel free to ask a specific question or describe an issue you would like to report.";
    }

    /**
     * AI-Powered Grievance Drafting & Refinement
     */
    public Map<String, Object> refineGrievance(String rawInput, String currentCategory) {
        if (rawInput == null || rawInput.isBlank()) {
            return Map.of(
                    "category", currentCategory != null && !currentCategory.isBlank() ? currentCategory : "Road Damage",
                    "refinedDescription", "Please provide a brief description of the civic problem you observed.",
                    "suggestedWard", "Ward 1",
                    "priority", "MEDIUM"
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            return fallbackRefineGrievance(rawInput, currentCategory);
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();

            ObjectNode systemInstruction = requestBody.putObject("system_instruction");
            systemInstruction.putArray("parts").addObject().put("text", """
                    You are NagarSeva AI, an expert municipal grievance drafting assistant.
                    Given a citizen's informal, rough, or incomplete input about a civic problem, produce a clear, professional, well-structured municipal grievance report.
                    
                    Available Categories: Streetlight, Road Damage, Drainage, Illegal Dumping, Unsafe Area, Encroachment.
                    Available Wards: Ward 1, Ward 2, Ward 3.
                    
                    Respond strictly in valid JSON format with NO markdown wrapper:
                    {
                      "category": "One of the 6 valid categories",
                      "refinedDescription": "A polished, formal 2-3 sentence complaint describing the issue, public impact/hazard, and requested municipal remedy",
                      "suggestedWard": "Ward 1 or Ward 2 or Ward 3",
                      "priority": "HIGH or MEDIUM or LOW"
                    }
                    """);

            ArrayNode contentsArray = requestBody.putArray("contents");
            ObjectNode userNode = contentsArray.addObject();
            userNode.put("role", "user");
            userNode.putArray("parts").addObject().put("text", "Citizen input: " + rawInput + (currentCategory != null ? " (Selected category: " + currentCategory + ")" : ""));

            String responseBody = executeWithFallback(requestBody);
            JsonNode root = objectMapper.readTree(responseBody);
            String text = extractTextFromGeminiResponse(root);
            JsonNode json = parseCleanJson(text);

            String category = json.path("category").asText(currentCategory != null && !currentCategory.isBlank() ? currentCategory : "Road Damage");
            String refinedDescription = json.path("refinedDescription").asText(rawInput);
            String suggestedWard = json.path("suggestedWard").asText("Ward 1");
            String priority = json.path("priority").asText("MEDIUM");

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("category", category);
            result.put("refinedDescription", refinedDescription);
            result.put("suggestedWard", suggestedWard);
            result.put("priority", priority);
            return result;
        } catch (Exception e) {
            log.error("Gemini grievance refinement failed: {}", e.getMessage());
        }

        return fallbackRefineGrievance(rawInput, currentCategory);
    }

    private Map<String, Object> fallbackRefineGrievance(String rawInput, String currentCategory) {
        String lower = rawInput.toLowerCase();
        String category = currentCategory != null && !currentCategory.isBlank() ? currentCategory : "Road Damage";
        String priority = "MEDIUM";

        if (lower.contains("pothole") || lower.contains("road") || lower.contains("crack") || lower.contains("asphalt")) {
            category = "Road Damage";
            priority = "HIGH";
        } else if (lower.contains("light") || lower.contains("dark") || lower.contains("bulb") || lower.contains("lamp") || lower.contains("pole")) {
            category = "Streetlight";
            priority = "MEDIUM";
        } else if (lower.contains("water") || lower.contains("drain") || lower.contains("sewer") || lower.contains("leak") || lower.contains("pipe")) {
            category = "Drainage";
            priority = "HIGH";
        } else if (lower.contains("garbage") || lower.contains("dump") || lower.contains("trash") || lower.contains("waste")) {
            category = "Illegal Dumping";
            priority = "MEDIUM";
        } else if (lower.contains("unsafe") || lower.contains("crime") || lower.contains("safety") || lower.contains("harass")) {
            category = "Unsafe Area";
            priority = "HIGH";
        } else if (lower.contains("encroach") || lower.contains("stall") || lower.contains("block")) {
            category = "Encroachment";
            priority = "MEDIUM";
        }

        String refined = "Urgent civic redressal required: " + rawInput.trim() + ". This condition is causing significant public inconvenience and safety hazards to local residents and commuters. Prompt inspection and repair action by the concerned municipal authority is requested.";

        Map<String, Object> res = new java.util.HashMap<>();
        res.put("category", category);
        res.put("refinedDescription", refined);
        res.put("suggestedWard", "Ward 1");
        res.put("priority", priority);
        return res;
    }
}
