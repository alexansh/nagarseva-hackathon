"""
NagarSeva ML Service - Dataset Manager & Preparation
Organizes, splits, and inspects civic grievance training data.
"""

import os
import sys
import shutil
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np

# Ensure UTF-8 output on Windows consoles
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

# Standard Civic Grievance Categories aligned with NagarSeva
CIVIC_CLASSES = [
    "Road_Damage",        # Potholes, asphalt cracks, damaged pavement
    "Illegal_Dumping",    # Garbage heaps, overflowing bins, plastic waste
    "Drainage",           # Clogged drains, open sewer lines, waterlogging
    "Streetlight",        # Damaged poles, broken fixtures, exposed wiring
    "Encroachment",       # Footpath blockage, unauthorized structures
    "Non_Civic_Spam"      # Irrelevant photos: selfies, memes, pets, indoor items
]

BASE_DIR = Path(__file__).resolve().parent
DATASET_DIR = BASE_DIR / "dataset"

def setup_dataset_structure(dataset_dir: Path = DATASET_DIR):
    """Creates the standard ImageFolder train/val/test directory structure."""
    splits = ["train", "val", "test"]
    for split in splits:
        for class_name in CIVIC_CLASSES:
            target_path = dataset_dir / split / class_name
            target_path.mkdir(parents=True, exist_ok=True)
    print(f"✅ Created dataset directory structure at: {dataset_dir}")

def generate_sample_dataset(dataset_dir: Path = DATASET_DIR, samples_per_class: int = 15):
    """
    Populates sample images with visual visual representations for each class.
    This allows immediate testing, training pipeline verification, and demonstration.
    """
    setup_dataset_structure(dataset_dir)
    
    # Palette configuration per class
    palettes = {
        "Road_Damage": [(70, 70, 70), (45, 45, 45), (100, 90, 80)], # Asphalt tones
        "Illegal_Dumping": [(140, 100, 60), (90, 120, 60), (180, 80, 60)], # Trash/plastic tones
        "Drainage": [(40, 80, 110), (30, 60, 80), (60, 100, 120)], # Murky water tones
        "Streetlight": [(30, 35, 50), (20, 20, 30), (110, 160, 220), (85, 140, 205), (140, 185, 235)], # Night corridors and daylight blue sky
        "Encroachment": [(160, 130, 90), (180, 100, 50), (120, 120, 120)], # Brick/stall tones
        "Non_Civic_Spam": [(220, 180, 170), (240, 220, 150), (210, 190, 200)] # Random indoor wallpapers / domestic tones
    }

    random.seed(42)
    np.random.seed(42)

    total_generated = 0
    for class_name in CIVIC_CLASSES:
        colors = palettes.get(class_name, [(100, 100, 100)])
        
        # Split: 70% train, 15% val, 15% test
        n_train = int(samples_per_class * 0.70)
        n_val = int(samples_per_class * 0.15)
        n_test = samples_per_class - n_train - n_val

        split_counts = [("train", n_train), ("val", n_val), ("test", n_test)]

        for split, count in split_counts:
            target_dir = dataset_dir / split / class_name
            for i in range(count):
                img_path = target_dir / f"{class_name.lower()}_{split}_{i+1:03d}.jpg"
                if not img_path.exists():
                    # Generate a 224x224 sample image with patterns reflecting class
                    bg_color = random.choice(colors)
                    # Add subtle texture noise
                    noise = np.random.randint(-25, 25, (224, 224, 3), dtype=np.int16)
                    base_arr = np.clip(np.full((224, 224, 3), bg_color, dtype=np.int16) + noise, 0, 255).astype(np.uint8)
                    
                    img = Image.fromarray(base_arr)
                    draw = ImageDraw.Draw(img)

                    # Distinctive features per category
                    if class_name == "Road_Damage":
                        # Draw pothole or crack
                        draw.ellipse([50, 70, 170, 150], fill=(25, 25, 25), outline=(15, 15, 15))
                        draw.line([20, 110, 80, 125, 140, 115, 200, 130], fill=(15, 15, 15), width=3)
                    elif class_name == "Illegal_Dumping":
                        # Draw garbage pile polygons
                        for _ in range(5):
                            x1, y1 = random.randint(40, 120), random.randint(100, 180)
                            draw.rectangle([x1, y1, x1+35, y1+25], fill=random.choice([(200, 50, 50), (230, 230, 230), (50, 180, 80)]))
                    elif class_name == "Drainage":
                        # Draw gutter / puddle
                        draw.rectangle([0, 130, 224, 224], fill=(20, 45, 60))
                        draw.line([0, 130, 224, 130], fill=(90, 120, 140), width=4)
                    elif class_name == "Streetlight":
                        # Support both daytime blue-sky fixtures and night fixtures
                        is_daytime = bg_color[0] > 70 and bg_color[2] > 180
                        if is_daytime:
                            # Daytime: pole, bracket, lamp head with exposed bulb / shattered globe against sky
                            draw.rectangle([95, 90, 118, 224], fill=(60, 65, 75))
                            draw.line([106, 90, 155, 45], fill=(60, 65, 75), width=7)
                            draw.rectangle([135, 40, 165, 60], fill=(50, 55, 65))
                            draw.ellipse([128, 48, 172, 98], outline=(220, 235, 255), width=2)
                            draw.ellipse([140, 58, 160, 82], fill=(255, 245, 180))
                        else:
                            # Nighttime: vertical pole and glowing lantern
                            draw.rectangle([105, 40, 118, 224], fill=(70, 75, 85))
                            draw.ellipse([90, 25, 133, 60], fill=(255, 235, 120))
                    elif class_name == "Encroachment":
                        # Draw street stall / barricade
                        draw.rectangle([40, 80, 184, 170], fill=(180, 60, 40))
                        draw.polygon([(40, 80), (112, 40), (184, 80)], fill=(220, 180, 50))
                    elif class_name == "Non_Civic_Spam":
                        # Draw generic smiley or indoor shape
                        draw.ellipse([60, 60, 164, 164], fill=(240, 200, 120))
                        draw.ellipse([85, 95, 100, 110], fill=(30, 30, 30))
                        draw.ellipse([124, 95, 139, 110], fill=(30, 30, 30))
                        draw.arc([85, 115, 139, 145], 0, 180, fill=(30, 30, 30), width=3)

                    # Text stamp
                    draw.text((8, 8), f"NagarSeva: {class_name}", fill=(255, 255, 255))
                    img.save(img_path, "JPEG", quality=90)
                    total_generated += 1

    print(f"✅ Generated {total_generated} initial dataset samples across {len(CIVIC_CLASSES)} classes.")

def inspect_dataset(dataset_dir: Path = DATASET_DIR):
    """Prints a statistical summary of the dataset."""
    print("=" * 60)
    print("NAGARSEVA CIVIC DATASET SUMMARY")
    print("=" * 60)
    
    total_images = 0
    splits = ["train", "val", "test"]
    stats = {c: {s: 0 for s in splits} for c in CIVIC_CLASSES}

    for split in splits:
        split_dir = dataset_dir / split
        if not split_dir.exists():
            continue
        for class_name in CIVIC_CLASSES:
            class_dir = split_dir / class_name
            if class_dir.exists():
                count = len(list(class_dir.glob("*.jpg")) + list(class_dir.glob("*.png")) + list(class_dir.glob("*.jpeg")))
                stats[class_name][split] = count
                total_images += count

    print(f"{'Category':<22} | {'Train':<8} | {'Val':<8} | {'Test':<8} | {'Total':<8}")
    print("-" * 60)
    for c in CIVIC_CLASSES:
        tr = stats[c]["train"]
        va = stats[c]["val"]
        te = stats[c]["test"]
        tot = tr + va + te
        print(f"{c:<22} | {tr:<8} | {va:<8} | {te:<8} | {tot:<8}")
    print("-" * 60)
    print(f"Total Dataset Images: {total_images}")
    print("=" * 60)
    return stats

if __name__ == "__main__":
    generate_sample_dataset()
    inspect_dataset()
