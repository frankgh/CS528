/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
'use strict';

// [START all]
// [START import]
// The Cloud Functions for Firebase SDK to create Cloud Functions and setup triggers.
const functions = require('firebase-functions');

// The Firebase Admin SDK to access the Firebase Realtime Database. 
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);
// [END import]

// [START addMessage]
// Take the text parameter passed to this HTTP endpoint and insert it into the
// Realtime Database under the path /messages/:pushId/original
// [START addMessageTrigger]
exports.addMessage = functions.https.onRequest((req, res) => {
// [END addMessageTrigger]
  // Grab the text parameter.
  const name = req.query.text;
  const timestamp = parseInt(req.query.timestamp, 10);
  const type = parseInt(req.query.type, 10);
  // [START adminSdkPush]
  // Push the new message into the Realtime Database using the Firebase Admin SDK.
  admin.database().ref('/parking-events/dMBAAuRukvdK1uk0d0khEVEsgJ23').push({lotName: name, timestamp: timestamp, type: type}).then(snapshot => {
    // Redirect with 303 SEE OTHER to the URL of the pushed object in the Firebase console.
    res.redirect(303, snapshot.ref);
  });
  // [END adminSdkPush]
});
// [END addMessage]


// [START updateAvailableLots]
// Listens for new events added to /parking-events/:uid/:pushId and updates the available parking lots
exports.updateAvailableLots = functions.database.ref('/parking-events/{uid}/{pushId}')
    .onWrite(event => {
      const ev = event.data.val();
      const lotRef = admin.database().ref('/lots/' + ev.lotName);
	  
	  if (ev.type === 1) { // EXIT		  
		  // Remove last note after exiting
		  const notesRef = admin.database().ref('/notes/' + event.params.uid);
		  notesRef.remove();
		  // Remove last picture after exiting
		  var bucket = admin.storage().bucket();
		  var file = bucket.file('/noteImages/'+ event.params.uid + '/image.jpg');
		  file.delete();
	  }

      return lotRef
        .once('value')
        .then(snap => {
            if (snap.val()) {
                const capacity = snap.val().capacity;
                return snap.ref.child('used')
                    .transaction(function(current_value) {
                        if (ev.type === 0) { // ENTER
                            return Math.min(capacity, (current_value || 0) + 1);
                        } else if (ev.type === 1) { // EXIT
                            return Math.max(0, (current_value || 0) - 1);
                        }
                    });
            }
         });
    });
// [END updateAvailableLots]
// [END all]