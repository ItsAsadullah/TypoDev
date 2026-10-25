# TypoDev OTP Autofill

On Android 9 or newer, open TypoDev Settings → Clipboard → TypoDev OTP Autofill and select TypoDev in Android's Autofill provider picker. Google Play services is required. Android selects one Autofill provider, so choosing TypoDev replaces the current password/autofill provider; TypoDev's new service supplies OTPs only.

Focus an editable OTP field with the `smsOTPCode` autofill hint or an explicit OTP/verification-code label. Tap **Get SMS verification code** in Android's Autofill suggestion, accept Google's consent dialog if shown, and wait for the SMS. Android fills the code into that specific field. Cancel and a five-minute timeout return without filling anything. Requests fail closed if the target app is already retrieving SMS, Google denies access, or the field cannot be identified. This does not guarantee support in every app, browser, custom view, or split-digit OTP form.

For Chrome websites, also open Chrome → Settings → Autofill services → **Autofill using another service**, then restart Chrome. Selecting TypoDev in Android settings alone does not enable Chrome's third-party provider mode. TypoDev Settings → Clipboard → **Browser Autofill settings** opens the browser's configuration. See [Chrome help](https://support.google.com/chrome/answer/142893?co=GENIE.Platform%3DAndroid&amp;hl=en).

If no suggestion appears, open **Test OTP Autofill** in TypoDev's Clipboard settings. It provides a native OTP field and a **Request OTP Autofill** button, and shows whether Android contacted the service, exposed an OTP field, returned a Google API error/denial, or accepted the suggestion response. This test sends no SMS. If the native test works but a website does not, check browser settings and website Autofill compatibility. Field detection also understands camelCase IDs, accessible labels, HTML `autocomplete="one-time-code"`, and generic code fields on a verification screen; numeric fields alone are not treated as OTPs. A group of four to eight single-character editable fields on a recognized OTP screen is filled one character per field, in view order, only when a member is focused and the code length matches. This depends on the browser exposing each field through Android Autofill.

TypoDev does not request SMS permissions. The previous permission-based **Auto-copy SMS OTP** feature, receiver, and permission screen have been removed. OTP retrieval uses Google's consent-based Autofill API without copying codes to the clipboard, storing codes, or logging SMS contents. Native IME inline Autofill suggestions are not part of this change; the new provider currently uses Android's field suggestion menu and authentication flow.

Phone verification checklist:

- Enable TypoDev as the Autofill service on a Google Play services device, then focus an OTP field before and after receiving a real verification SMS.
- Test first-use consent, permission denial, back/cancel, rotation during consent and while waiting, and timeout.
- Verify leading-zero and alphanumeric codes are filled intact; normal PIN/password/numeric fields should receive no suggestion.
- Verify an app with its own active SMS Retriever gets no competing TypoDev prompt.
- Confirm the app requests no SMS permission and Google Autofill works through its own consent flow.
- Switch back to another Autofill provider and confirm TypoDev no longer retrieves codes through this API.

References: [Build Autofill services](https://developer.android.com/identity/autofill/autofill-services#sms-autofill), [SmsCodeAutofillClient](https://developers.google.com/android/reference/com/google/android/gms/auth/api/phone/SmsCodeAutofillClient).
