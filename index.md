Have you ever struggled to read analogue clocks? Don't be embarrassed, you can use my Automatic Analogue Clock Reader! *(Note: App not deployed and was never meant for production use)*

![Analogue Clock Reader](/clock_example.gif)

This was built in Android Studio with OpenCV Java. Code found [here](github.com/wongkj12/clock-reader).

The following examples will refer to OpenCV in Python for ease of explanation.

## How it works

In a nutshell, the algorithm works by using OpenCV's (an image processing library) various filters (see: [morphological transformations](https://docs.opencv.org/master/d9/d61/tutorial_py_morphological_ops.html), [geometric transformations](https://docs.opencv.org/4.5.2/da/d6e/tutorial_py_geometric_transformations.html)) to eventually extract the features that we want, i.e. clock hands, in order to read the time.

    import cv2

### 1. Detect and crop out clock face

Assuming that most clock faces are perfect circles, we can make use of [Hough Circle Transform](https://docs.opencv.org/3.4/d4/d70/tutorial_hough_circle.html):

    img = cv2.medianblur(img, 5) #Reduces noise before detection
    circles = cv2.HoughCircles(img, cv2.HOUGH_GRADIENT, 1, min_dist=width/16,
                        param_1=100, param_2=30, minRadius=50, maxRadius=100)
                               
 `circles[0][0]` will return an array containing the radius & coordinates of the center of the best circle detected.
 
 ### 2. "Unroll" the clock face using warpPolar
 
 We can apply a warpPolar transformation, which remaps an image to polar coordinates space. Loosely speaking, this is what it means:
 
 ![warpPolar](/warpPolar.png)
 
        warped = cv2.warpPolar(img, dsize=(0,0), center=center,
                        maxRadius=radius, flags=cv2.WARP_POLAR_LINEAR))
                        


  
    

    








## Things to work on

- Currently the program may erratically swap the minute & hour hands - not sure why
- Only works on clocks with a white face and black clock hands (easy fix - check brightness value of face/clock hands before thresholding)
- May get confused with clocks with larger second hands (Ideally the second hand should get ignored after dilating)
- Method of detecting clock face by searching for circles is naive. Maybe try to implement an object detection model?
