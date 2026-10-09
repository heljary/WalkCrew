import * as admin from "firebase-admin";
import { onDocumentCreated } from "firebase-functions/v2/firestore";

admin.initializeApp();

/**
 * Cloud Function triggered when a new Nudge is created in a Crew.
 * Reads the recipient's FCM token from users/{toUserId} and dispatches
 * a push notification with a friendly encouragement message.
 */
export const onNudgeCreated = onDocumentCreated(
  "crews/{crewId}/nudges/{nudgeId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      console.log("No data associated with nudge event");
      return;
    }

    const data = snapshot.data();
    const toUserId = data.toUserId as string;
    const fromUserName = (data.fromUserName as string) || "A teammate";
    const crewName = (data.crewName as string) || "your crew";
    const message = (data.message as string) || `${fromUserName} thinks you can catch up today! 👟`;

    try {
      // 1. Retrieve the recipient's FCM device token
      const userDoc = await admin.firestore().collection("users").doc(toUserId).get();
      if (!userDoc.exists) {
        console.log(`User ${toUserId} does not exist in Firestore`);
        return;
      }

      const userData = userDoc.data();
      const fcmToken = userData?.fcmToken as string | undefined;

      if (!fcmToken) {
        console.log(`User ${toUserId} does not have an active FCM token registered`);
        return;
      }

      // 2. Dispatch FCM Push Notification payload
      const payload: admin.messaging.Message = {
        token: fcmToken,
        notification: {
          title: `WalkCrew • ${fromUserName} nudged you!`,
          body: message,
        },
        data: {
          type: "nudge",
          crewId: event.params.crewId,
          fromUserId: data.fromUserId || "",
          click_action: "OPEN_CREW_DETAIL",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "walkcrew_nudges",
            icon: "ic_walkcrew_logo_1791431697293",
            color: "#0E3B2C",
            defaultSound: true,
            defaultVibrateTimings: true,
          },
        },
      };

      const response = await admin.messaging().send(payload);
      console.log(`Successfully dispatched nudge notification to ${toUserId}:`, response);
    } catch (error) {
      console.error(`Error sending nudge push notification to ${toUserId}:`, error);
    }
  }
);
