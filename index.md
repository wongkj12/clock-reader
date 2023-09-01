Have you ever struggled to read analogue clocks? Don't be embarrassed, you can use my Automatic Analogue Clock Reader! 

![Analogue Clock Reader](/clock_example.gif)

This was built in Android Studio with OpenCV Java. Code found [here](https://github.com/wongkj12).

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
 
### 2. "Unroll" the clock using warpPolar
 
We can apply a [warpPolar](https://docs.opencv.org/3.4/da/d54/group__imgproc__transform.html#ga49481ab24fdaa0ffa4d3e63d14c0d5e4) transformation, which remaps an image to polar coordinates space. Loosely speaking, this is what it means:
 
 ![warpPolar](/warpPolar.png)
 
    warped = cv2.warpPolar(img, dsize=(0,0), center=center,
                    maxRadius=radius, flags=cv2.WARP_POLAR_LINEAR))
                        
 ![warpPolar2](/warpPolar2.png)
 
The minute and second hands can be identified by the two thickest blobs pointing towards the right. This might make it easier for our algorithm to read the hands as they are now simply pointing horizontally rather than radiating from the centre at varying angles.

### 3. Dilation

The dilation operation essentially makes the bright parts of an image grow. This generally helps to remove the second hand (as it is too thin), as well as uneccessary features like the numbers on the clock face. We can apply a [dilation](https://docs.opencv.org/3.4/db/df6/tutorial_erosion_dilatation.html) to the whole image:

    # kernel will be a 1xW rectangle
    # where W = 15% of the warped image's width
    kernel_width = int(round(0.15*warped.shape[1]))
    kernel = np.ones((1,kernel_width),np.uint8)
    dil = cv2.dilate(warped,kernel,1)

![dilation](/dilation.png)

Right now, it's quite clear to us which blobs represent the minute and hour hands, but still not easy enough for the computer to read.

### 4. Make it black-and-white with (Inverse) Binary Thresholding

Next, we'll apply an Inverse [Binary Threshold](https://docs.opencv.org/3.4/db/d8e/tutorial_threshold.html), which basically makes the bright parts of the image completely black, and the dark parts of the image completely white:

    thresh = 100
    maxValue = 255
    th,binary = cv2.threshold(cv2.cvtColor(dil,cv2.COLOR_BGR2GRAY), thresh, maxValue, cv2.THRESH_BINARY_INV)

![binary_threshold](/binary_threshold.png)

Now it'll be much easier for the computer to recognise the desired features given a binary image.

### 5. Detect the "blobs" by finding image contours

What we have in our transformed image right now are [contours](https://docs.opencv.org/3.4/d4/d73/tutorial_py_contours_begin.html), curves joining all the continuous points along the boundary of an area, having the same color or intensity. These are very easy to detect using OpenCV:

    contours, hierarchy = cv2.findContours(binary, cv2.RETR_TREE, cv2.CHAIN_APPROX_SIMPLE)

We'll sort the contours of the image by area in order to find the clock hands. Theoretically, the two largest contours left on the image should only be the hour and minute hands since we had done all that filtering.

     contours.sort(key = cv2.contourArea,reverse = True)

In order to differentiate between the hour and minute hands, consider the contours' max x-values. Since the minute hand is longer, it's maximum x-value pixel should be greater:

    max0 = max(pix[0][0] for pix in contours[0])
    max1 = max(pix[0][0] for pix in contours[1])
    if max0 >= max1:
        minute = contours[0]
        hour = contours[1]
    else:
        minute = contours[1]
        hour = contours[0]

### 6. Calculations
 
And we are pretty much done! We can work backwards to find the approximate time by using the relative average y-values of the contours we found. After that, with some troublesome trigonometry we can redraw the clock hands on our original image:

![final_clock](/final_clock.png)

Nice! We never have to read another analogue clock again.
     
 
 


    




 
 
                        
                        


  
    

    








## Things to work on

- Currently the program may erratically swap the minute & hour hands - not sure why
- Only works on clocks with a white face and black clock hands (easy fix - check brightness value of face/clock hands before thresholding)
- May get confused with clocks with larger second hands (Ideally the second hand should get ignored after dilating)
- Method of detecting clock face by searching for circles is naive. Maybe try to implement an object detection model?
