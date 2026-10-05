import sys
from pathlib import Path
from PIL import Image

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

def main():
    src_img_path = Path(r"C:\Users\saras\.gemini\antigravity\brain\9f2c5177-46bc-4acc-bd49-bc92db5104a9\.user_uploaded\media_1791219988415.jpg")
    if not src_img_path.exists():
        print(f"Error: {src_img_path} does not exist")
        return

    img = Image.open(src_img_path).convert("RGB")
    w, h = img.size

    dataset_dir = Path(__file__).resolve().parent / "dataset"
    train_dir = dataset_dir / "train" / "Streetlight"
    val_dir = dataset_dir / "val" / "Streetlight"
    test_dir = dataset_dir / "test" / "Streetlight"

    train_dir.mkdir(parents=True, exist_ok=True)
    val_dir.mkdir(parents=True, exist_ok=True)
    test_dir.mkdir(parents=True, exist_ok=True)

    # 1. Full image resized to 224x224
    img.resize((224, 224), Image.Resampling.LANCZOS).save(train_dir / "real_streetlight_full_train.jpg", "JPEG", quality=95)
    img.resize((224, 224), Image.Resampling.LANCZOS).save(val_dir / "real_streetlight_full_val.jpg", "JPEG", quality=95)

    # 2. Crops centered on the broken glass fixture and bulb
    # Center crop
    crop_center = img.crop((w * 0.15, h * 0.15, w * 0.85, h * 0.85)).resize((224, 224), Image.Resampling.LANCZOS)
    crop_center.save(train_dir / "real_streetlight_crop_center.jpg", "JPEG", quality=95)

    # Upper crop (fixture + sky)
    crop_upper = img.crop((0, 0, w, h * 0.75)).resize((224, 224), Image.Resampling.LANCZOS)
    crop_upper.save(train_dir / "real_streetlight_crop_upper.jpg", "JPEG", quality=95)

    # Lower crop (bracket + pole + sky)
    crop_lower = img.crop((w * 0.1, h * 0.25, w * 0.9, h)).resize((224, 224), Image.Resampling.LANCZOS)
    crop_lower.save(train_dir / "real_streetlight_crop_lower.jpg", "JPEG", quality=95)

    # Horizontal flip crop
    crop_flipped = crop_center.transpose(Image.FLIP_LEFT_RIGHT)
    crop_flipped.save(train_dir / "real_streetlight_crop_flipped.jpg", "JPEG", quality=95)
    crop_flipped.save(test_dir / "real_streetlight_crop_test.jpg", "JPEG", quality=95)

    print("✅ Added authentic daytime streetlight samples to train, val, and test splits.")

if __name__ == "__main__":
    main()
