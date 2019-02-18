==================
Build caffe trên Ubuntu theo hướng dẫn: 
https://github.com/BVLC/caffe/wiki/Ubuntu-16.04-or-15.10-Installation-Guide

==================
caffe-android-demo
==================

Sử dụng mô hình được training theo Caffe, Alexnet
Copy file mô hình và các file cấu hình mô hình vào điện thoại theo đường dẫn:
/sdcard/caffe_mobile/bvlc_reference_caffenet/plantvillage.caffemodel
/sdcard/caffe_mobile/bvlc_reference_caffenet/solver.prototxt
/sdcard/caffe_mobile/bvlc_reference_caffenet/deploy.prototxt

+) file assets/synset_words.txt chứa tên các bệnh
+) thư mục jniLibs chứa thư viện libcaffe.so và libcaffe_jni.so được build từ https://github.com/sh1r0/caffe-android-lib chứa các hàm, phương thức đọc và sử dụng file mô hình caffemodel.
+) NOTE: khi chạy ứng dụng, cần cấp quyền truy cập Bộ nhớ và Máy ảnh cho ứng dụng.


*) Gửi dữ liệu từ Android lên Thingsboard Server.
        Note: Muon MQTT chay duoc phai cap phep o file AndroidManifest.xml
        Thêm trước dòng </application>
        <service android:name="org.eclipse.paho.android.service.MqttService" />
    Hàm sử dụng: publishMessage()
*) Sau khi có kết quả phát hiện, kết quả ở hàm onTaskCompleted(), nếu lá bị bệnh sẽ gửi ảnh lên firebase uploadImage() và gửi bản tin lên Thingsboard Server.
*) Cấp quyền, yêu cầu cấp quyền cho ứng dụng: (File AndroidManifest.xml)
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

    Chú ý: <!-- Services required for using MQTT --> Cần có để gửi bản tin qua mqtt
        <service android:name="org.eclipse.paho.android.service.MqttService" />

*) Thư viện và các phụ thuộc:
        -) Thư viện libcaffe.so và libcaffe_jni.so được build theo hướng dẫn: https://github.com/sh1r0/caffe-android-lib 
        -) build.grandle (App):
    apply plugin: 'com.google.gms.google-services' 
    --> dùng cho gửi ảnh lên firebase

    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
    implementation('org.eclipse.paho:org.eclipse.paho.android.service:1.1.0') {
        exclude module: 'support-v4'
    }
    ---> dùng gửi bản tin mqtt android 

    implementation 'com.google.android.gms:play-services-analytics:11.4.2'
    implementation 'com.github.yesidlazaro:GmailBackground:1.2.0'
    ---> gửi mail (Chưa dùng trong app)

    implementation 'com.android.support.constraint:constraint-layout:1.0.2'
    testImplementation 'junit:junit:4.12'

    implementation 'com.google.firebase:firebase-messaging:11.4.2'
    implementation 'com.google.firebase:firebase-auth:11.4.2'
    implementation 'com.google.firebase:firebase-storage:11.4.2'

    implementation 'com.sendgrid:sendgrid-java:4.3.0'
    implementation 'com.sendgrid:java-http-client:4.2.0'
        implementation 'org.apache.httpcomponents:httpcore:4.4.4'
    testImplementation 'org.mockito:mockito-core:1.10.19'
    testImplementation 'junit:junit-dep:4.10'
    implementation 'org.apache.httpcomponents:httpclient:4.5.7'
    implementation ('net.sargue:mailgun:1.9.0'){
        exclude group: 'javax.inject', module: 'javax.inject'
    }
    implementation 'org.glassfish.jersey.core:jersey-client:2.25'
    implementation 'org.glassfish.jersey.media:jersey-media-multipart:2.25'
    implementation 'com.android.support:multidex:1.0.1'
    implementation 'org.glassfish.hk2.external:javax.inject:2.4.0-b34'
    ---> gửi mail (Chưa dùng trong app, có thế dùng một trong ba: sendgrid hoặc GmailBackground hoặc mailgun ở trên)

