# Audio Flow
![audio-flow-logo-wide](https://github.com/danuo/audio-flow/assets/66017297/8069df1d-0ebf-4176-b187-8ee59a04b40c)

Audio Flow is an Android app designed for music artists to help achieving a consistent audio level thoughout their performance. Different songs and instruments usually introduce fluctuating volume levels, creating an extra task for musicians trying to produce professional sound. Modern audio mixers come with a audio level display, but they only show peak volume in coarse steps. With a huge range of 3 dB per LED on the level display, audible fluctuations can remain unnoticed for the artist.

Audio Flow features a fine grained level display and real time data charts. Multiple metrics such as peak volume and rms volume are shown in a real time plot, enabling artists to compare the volume level throughout their entire performance. This makes music sets at consistent audio levels or with intended shifts achievable at ease.

Audio Flow can also be used by audio engineers such as foh technicians to monitor and master an artist's performance. The web ui can help in this use case, as it enables remote moinitoring. The Android phone running Audio Flow may be placed with the musician, while the audio engineer monitors the audio level from another place by using another phone or a notebook, using the web ui via Wifi.

# Features
* 🎤 Fine grained level display with 1dB range per LED
* 📉 Real time chart showing the audio level over time. Shown time range can be selected between 10 minutes and 6 hours.
* 💾 Audio data is stored in a sqlite database so data is still present after restarting or pausing the app.
* 🔗 Audio time data can also be viewed through a web ui. Enable the web ui in the settings and view the data from any device in the locan area network.


# Usage
![imgonline-com-ua-dexifUuJwSNrkhtbX](https://github.com/danuo/audio-flow/assets/66017297/f5bf0407-3003-4847-bc00-9b830d03dd06)

Connect audio in jack your Android phone running Audio Flow with the master out of the mixer. In case of clipping, lower the master out level by a constant factor until clipping is not happening anymore (us a dsp for example). In the app, use the level adjust option to calibrate the audio level shown in the app. Depending on the connectivity options of your phone and your mixer, you might have to purchase suitable adapter cables for the connection (usb-c / chinch / xlr etc.).

# Example
<table width="100%">
  <tr>
  <td width="40%"><img src="https://github.com/danuo/audio-flow/assets/66017297/2333c449-0a35-40e0-ad6c-b50cec8c7b95" /></td>
  <td width="50%">The following screenshot shows the app in use. The led bar on the right is a level display showing the current peak amplitude. The chart on to the left shows the same information for a selectable timeframe. The chart and the led bar are vertically aligned, meaning that at each height they show the same amplitude.</td>
  </tr>
</table>
