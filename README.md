# Voicemail Example for Android
Clone of https://code.google.com/p/voicemail-example-for-android/  
Main author is [@flerda](https://github.com/flerda)

## Description
This package contains an example implementation of a voicemail source for
Android.

The source takes care of synchronizing the voicemails stored locally on the
device with the ones stored remotely in a voicemail service. The voicemail
service this application is meant to talk to is based on the OMTP specification
for Visual Voicemail, and comprises of two main components: an IMAP server,
storing the actual voicemails as audio attachments to email messages; and an
SMS server, which sends and receives binary SMS to communicate with the device.

This implementation is neither complete nor production-ready, but it is only a
sample implementation used to provide an example of how the Voicemail related
APIs on Android can be used to integrate a voicemail service.

----
## License

Copyright 2011 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
