# FaceAppCycleGAN

Bachelors project - mobile application designed to change face emotion (from happy to neutral and vice versa) using CycleGAN.

[Official CycleGAN github project (and links to paper and other works)](https://github.com/junyanz/pytorch-CycleGAN-and-pix2pix)  
[Implementation used in this project](https://github.com/xhujoy/CycleGAN-tensorflow)  

Mobile application (Android) is written in Java with the use of Tensorflow (CycleGAN module). 
Deep learning model weight were frozen and added to mobile application in .pb file.  

![Simple project's architecture (in Polish)](https://drive.google.com/uc?export=view&id=1G8h5FPWkK7tweqo80AJ6bGiZkhIv8WkE)  

Mobile appilication lets user to pick a photo from gallery or to take a photo. 
Then it detects face (with the use of FirebaseVisionFaceDetector), processes it and generates the miniature of changed face expression.  
