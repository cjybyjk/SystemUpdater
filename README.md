SystemUpdater
=======
Simple application to download and apply OTA packages.

Server requirements
-------------------
The app sends `GET` requests to the URL defined by the `attr_update_channel_url`
resource (or the `ro.updater.channel_url` system property) and expects as response
a JSON with the following structure:
```json
[
    { 
      "name":"Example ROM",
      "version":"1.0.0",
      "device":"generic_x86",
      "requirement":0 ,
      "type":"full",
      "url":"https://example.com/ota-package.zip",
      "description":"ZXhhbXBsZQ==",
      "timestamp":1565236714,
      "file_size":314572800,
      "sha1":"45eafb9e95ae5c79a74687499ba18474d6aa1cbd" 
    }
]
```

The `name` attribute is the name of this update.  
The `version` attribute is the version of this update.  
The `device` attribute is the string to be compared with the device name.  
The `requirement` attribute is the string to be compared with the system build timestamp (for incremental updates only).  
The `type` attribute is the type of the update (support: full, incremental).  
The `url` attribute is the URL of the file to be downloaded.  
The `description` attribute is the base64 encoded description of this update (HTML supported).  
The `timestamp` attribute is the build date expressed as UNIX timestamp.    
The `file_size` attribute is the size of the update expressed in bytes.  
The `sha1` attribute is the sha1sum result of the update file.  

Additional attributes are ignored.  

Build information 
-------------------------
You need generate a keystore and keystore.properties using `gen-keystore.sh` before build SystemUpdater.

### IMPORTANT NOTICE ###
* You should replace `res/drawable/update.png` with your own file
* Please edit `res/values/strings.xml` to set your attributes

### If you want to run this application on a non-root device, you need to do the following
1. Add android:sharedUserId="android.uid.system" to the application tag of AndroidManifest.xml
2. Sign the apk with the platform key
