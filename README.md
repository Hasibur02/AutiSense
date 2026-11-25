# AutiSense
### An Explainable Deep Learning-Based Android Application for Autism Spectrum Disorder Detection

AutiSense is an Android application developed as part of an undergraduate research project on Autism Spectrum Disorder (ASD) detection using facial images. The project explores the application of deep learning and Explainable Artificial Intelligence (XAI) to make image-based model predictions more interpretable.

## Project Overview

Autism Spectrum Disorder is a neurodevelopmental condition. This project investigates whether deep learning models can identify patterns in facial images associated with ASD and explores ways to explain their predictions.

AutiSense serves as an application-level demonstration of the research framework, connecting deep learning-based image classification with visual explanations.

## Research Background

The underlying research project, titled **"An Explainable Deep Learning Framework for Autism Spectrum Disorder (ASD) Detection from Facial Images,"** investigated several deep learning architectures.

The evaluated architectures include:

- AlexNet
- DenseNet121
- MobileNetV2
- MobileNetV3-Large
- RegNetY-800MF
- Vision Transformer (ViT-B/16)

## Model Performance

RegNetY-800MF achieved the best performance in the experiments.

| Metric | Result |
|---|---:|
| Accuracy | 93% |
| Precision | 93% |
| Recall | 93% |
| F1-Score | 93% |
| AUC | 0.9679 |

**Dataset:** 2,942 facial images.

These results represent the experimental performance of the research model and should not be interpreted as evidence of clinical diagnostic accuracy.

## Explainable AI

The project uses **Gradient-weighted Class Activation Mapping (Grad-CAM)** to visualize image regions that contribute to the model's predictions.

This approach provides visual insight into model behavior and helps make predictions easier to examine.

## Technologies

- Python
- PyTorch
- Deep Learning
- Convolutional Neural Networks (CNNs)
- Vision Transformer
- Computer Vision
- Grad-CAM
- Android Development

## Repository Structure

```text
AutiSense/
├── app/
├── gradle/
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle.kts
```

## Research Status

- Undergraduate research project: Completed
- Deep learning model experiments: Completed
- Android application: Developed
- Research manuscript: In preparation

## Important Disclaimer

AutiSense is an academic research prototype developed for educational and research purposes. It is not a clinically validated diagnostic system and must not be used as a substitute for professional medical assessment.

## Author

**Md. Hasibur Rahman**  
B.Sc. in Computer Science & Engineering  
Shyamoli Engineering College, University of Dhaka

- GitHub: [Hasibur02](https://github.com/Hasibur02)
- LinkedIn: [Md. Hasibur Rahman](https://linkedin.com/in/hasibur-rahman-a57a74428)

## Supervisor

**Golam Rabbi Hasan**  
Lecturer, Department of Computer Science & Engineering  
Shyamoli Engineering College

## Acknowledgement

This project was completed as an undergraduate research project under the supervision of Golam Rabbi Hasan.
