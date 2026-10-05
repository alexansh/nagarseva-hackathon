# NagarSeva: Project Presentation Script (4 Speakers)

**Project Title:** NagarSeva – AI-Powered Municipal Grievance Redressal & Civic Governance Platform  
**Target Duration:** 6 to 8 minutes (~1.5 to 2 minutes per speaker)  
**Tone:** Professional, clear, conversational, and easy to explain  

---

## 👥 Speaker Assignments

| Speaker | Primary Role | Key Focus Areas |
| :--- | :--- | :--- |
| **Speaker 1** | Problem Statement & Citizen Flow | Urban civic issues, spam challenge, Leaflet map, AI Assistant & Text Refinement |
| **Speaker 2** | Custom Machine Learning (Tier-1) | Edge MobileNetV2 model, <18ms latency, zero API cost, anti-spam gatekeeper, GitHub datasets (Tokyo Univ RDD2022, Imperial College TACO, Stanford TrashNet) |
| **Speaker 3** | Cognitive AI & Resolution Audit (Tier-2) | Gemini Vision, defect severity, lamppost OCR, tamper-proof before/after photo audit, photo forensics |
| **Speaker 4** | Backend Engineering & Conclusion | Spring Boot 3, PostgreSQL, real-time WebSockets, Municipal Dashboard, Public Tracking Portal, Safety Map |

---

## 🎭 The Complete Presentation Script

---

### **Speaker 1: The Problem & Citizen Portal**
*(Duration: ~1.5 - 2 minutes)*

> **[Speaker 1 steps forward]**
>
> "Good morning, respected supervisor and evaluators. Today, our team is excited to present **NagarSeva**, an intelligent, end-to-end municipal grievance redressal platform.
>
> In almost every Indian city, citizens struggle with recurring civic issues: broken roads, waterlogged streets, dark streetlights, and overflowing garbage. But traditional municipal complaint portals face two massive problems:
> 1. **Citizen friction:** Citizens don’t know which department handles which problem or which ward they belong to.
> 2. **Spam & fake reports:** People upload random photos—selfies, indoor rooms, or internet memes—flooding municipal offices with fake complaints.
>
> To solve this, we built NagarSeva around a **Two-Tier AI Architecture**. 
>
> Let’s look at the citizen's journey:
> - A citizen opens our platform on their phone. Right away, they can chat with our **Civic AI Assistant** to ask questions in plain English or Hindi.
> - When reporting an issue, they drop a pin on our **Interactive Leaflet Map**, and the system automatically determines their municipal ward.
> - If they write a vague or messy description, they click **'Refine with AI'**, and our model polishes it into a clean, professional municipal ticket.
>
> But the most critical step is **photo verification**. How do we know the uploaded photo is a real civic defect and not spam?
>
> For that, I’ll hand over to **[Speaker 2]**, who will explain our custom Machine Learning model."

---

### **Speaker 2: Custom Machine Learning (Tier-1 Edge Model)**
*(Duration: ~1.5 - 2 minutes)*

> **[Speaker 2 steps forward]**
>
> "Thank you, **[Speaker 1]**.
>
> One of our core engineering decisions was: **we did not want to rely solely on cloud AI APIs for every single image.** Sending thousands of photos to a cloud API is slow, expensive, and wastes server resources on spam.
>
> So, we built our own **Tier-1 Edge Computer Vision Model** using **Transfer Learning on MobileNetV2**:
> - Why MobileNetV2? It is extremely compact—only **3.4 million parameters** and a **9.1 megabyte** model file. It processes an image in under **18 milliseconds on a standard CPU**, meaning municipal offices don't need expensive supercomputers to run it.
>
> When a photo is uploaded, our model does two things instantly:
> 1. **Spam Rejection:** It acts as an automatic gatekeeper. If someone uploads a selfie, a meme, or a pet, it detects the negative control pattern and immediately rejects it with status `REJECTED_NON_CIVIC`.
> 2. **Defect Classification:** If it is a real issue, it classifies the defect across our 6 municipal categories with a confidence score.
>
> Now, where did our training data come from? We grounded our training in **three authoritative open-source research datasets from GitHub**:
> - For road defects and potholes, we used the **University of Tokyo's RDD2022 dataset** (`sekilab/RoadDamageDetector`), which contains 47,000 annotated road images from India and Japan.
> - For garbage and waste dumping, we integrated **Imperial College London’s TACO dataset** (`pedropro/TACO`) and **Stanford University’s TrashNet** (`garythung/trashnet`).
> - For spam filtering, we trained on negative control slices from **ImageNet-1K**.
>
> Our model achieved a **1.00 Macro F1-score** on our held-out test data with zero false positives on spam.
>
> Now, what happens after our ML model verifies the photo? **[Speaker 3]** will explain how our Tier-2 Cognitive AI takes over."

---

### **Speaker 3: Cognitive AI & Tamper-Proof Resolution Audit**
*(Duration: ~1.5 - 2 minutes)*

> **[Speaker 3 steps forward]**
>
> "Thanks, **[Speaker 2]**.
>
> Once our custom ML model confirms: *'Yes, this is genuine road damage with 99% confidence'*, the ticket moves to **Tier-2: Cognitive AI powered by Google Gemini Vision**.
>
> While our custom ML model is fast at classification, Gemini handles the **deep reasoning**:
> - It analyzes the severity: is this a minor crack or a dangerous deep pothole?
> - It performs **Visual OCR** to read street signs or printed lamppost ID numbers.
> - And it automatically routes the ticket to the exact municipal division.
>
> But we didn't stop at reporting. We also solved the biggest issue in municipal governance: **fake ticket closures.**
>
> In many cities, contractors mark tickets as 'Resolved' without actually fixing anything. In NagarSeva:
> - A municipal officer **cannot** close a ticket with just a checkbox.
> - The officer must upload a **Resolution Photo** of the completed work.
> - Our system runs **Photo Forensics** to check camera EXIF timestamps and verify GPS coordinates.
> - Then, our AI auditor compares the **original citizen photo** side-by-side with the **officer's resolution photo**. It checks if the pothole is genuinely filled with fresh asphalt and confirms background landmarks match.
>
> If the work isn't done, the ticket stays open. This makes NagarSeva completely transparent and fraud-proof.
>
> Now, **[Speaker 4]** will explain the backend engine and show how all of this connects."

---

### **Speaker 4: Backend Engineering, Live Dashboards & Impact**
*(Duration: ~1.5 - 2 minutes)*

> **[Speaker 4 steps forward]**
>
> "Thank you, **[Speaker 3]**.
>
> Powering this entire ecosystem is our backend built on **Java 17 and Spring Boot 3**:
> - **Robust REST API:** Handles user authentication, complaint submission, role-based access control (Citizen, Officer, Admin), and database transactions.
> - **Database Layer:** Uses **PostgreSQL** in production on the cloud, with an automated **H2 in-memory fallback** for offline development.
> - **Real-Time WebSockets:** When a citizen submits a report, the complaint instantly pops up on the **Municipal Officer Dashboard** without requiring a page refresh.
>
> We also built dedicated portals for every stakeholder:
> 1. **Municipal Officer Dashboard:** Where field teams track tickets, navigate to defect coordinates, and submit resolution proofs.
> 2. **Public Transparency Portal:** Any citizen can enter a ticket ID to see real-time progress and view the before-and-after photo evidence.
> 3. **Safety Heatmap:** Visualizes city-wide safety data, showing ward-level defect clusters to help city planners prioritize long-term infrastructure budgets.
>
> In summary, NagarSeva is not just a form and not just an API wrapper:
> - It combines **edge computer vision (MobileNetV2)** for fast, free local screening...
> - **Multimodal generative AI (Gemini)** for reasoning and fraud prevention...
> - And an **enterprise-grade Spring Boot architecture** for municipal scalability.
>
> All of our code, trained model weights, evaluation plots, and documentation are committed and live on our GitHub repository.
>
> Thank you, and we are now ready for your questions!"

---

## 🎯 Quick Delivery Checklist for the Team

1. **Speaker Hand-Offs:** Always verbally hand off to the next person: *"Now I'll hand over to [Name]..."*
2. **Terminal Demo (Keep ready in background):**
   ```powershell
   cd Nagar-Seva/ml-service
   python infer.py dataset/test/Road_Damage/road_damage_test_001.jpg
   python infer.py dataset/test/Non_Civic_Spam/non_civic_spam_test_001.jpg
   ```
3. **Plots to Open If Asked:**
   - `Nagar-Seva/ml-service/artifacts/loss_accuracy_curve.png` (Training convergence)
   - `Nagar-Seva/ml-service/artifacts/confusion_matrix.png` (Class accuracy)
