# CS528
MOBILE AND UBIQUITOUS COMPUTING
## Project 4

### HumanActivity
Classification of human activity (Walking, Climbing stairs, Sitting, Standing, Laying down) from accelerometer and gyroscope data.

Classify the activity using all classifier types that are available in the classification learner app. What is the 1) most accurate type of classifier and 2) Percentage accuracy when you use the following features:

1. Only the 3 original features (mean, PCA and Standard deviation)?

#### Classifier:

**Model Type**
Preset: Bagged Trees
Ensemble method: Bag
Learner type: Decision tree
Number of learners: 30

**PCA**
PCA disabled

#### Results:

| Accuracy         | 96.7%
| Prediction Speed | ~12000 obs/sec
| Training time    | 30.154 sec

2. 3 original features (mean, PCA, Standard deviation) and also Average Absolute Difference (i.e. 4 features in total)?

#### Classifier:
#### Accuracy:

3. 3 original features (mean, PCA, Standard deviation) and also Average Absolute Difference and Average Resultant Acceleration (i.e. 5 features in total)?

#### Classifier:
#### Accuracy:

4. 3 original features (mean, PCA, Standard deviation), and also Average Absolute Difference, Average Resultant Acceleration and Time Between Peaks (i.e. 6 features in total)?

#### Classifier:
#### Accuracy:

5. 3 original features (mean, PCA, Standard deviation) and also Average Absolute Difference, Average Resultant Acceleration, Time Between Peaks and Binned Distribution (i.e. 7 features in total)?

#### Classifier:
#### Accuracy: