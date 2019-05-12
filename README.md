# Masks

## What is it?

This is android application. It is VR application, it uses stream from camera, and change face in camera online. Here you can find examples how to work with camera, JNI, use machine learning libraries: opencv, dlib, how to work with OpenGL. Examples of result of working application you could see by video:<br/>
<a href="http://www.youtube.com/watch?feature=player_embedded&v=tnyJwTl7KT4
" target="_blank"><img src="http://img.youtube.com/vi/tnyJwTl7KT4/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>

# Table of Contests

- [What is it?](#what-is-it)
- [Settings](#settings)

## Settings

You should download trained shape predictor file from [here](http://dlib.net/files/shape_predictor_68_face_landmarks.dat.bz2) or could trian it by yourself. Unzip it, rename it to sp68.dat and put in dir [assets](https://github.com/oleg-sta/Masks/tree/master/assets). This file is very large so I don't store on version control. Yes, it's bad, but I made simple bad solution.
Make settings to submodule [commonLib](https://github.com/oleg-sta/commonLibMask).

## How it works

It uses my submodule [here](https://github.com/oleg-sta/commonLibMask) with all description of logic in it.
