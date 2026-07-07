# Device Info By Gad

A powerful, Jetpack Compose based Android application for monitoring hardware, software, network, battery, and system metrics.

## Building the project
Use standard gradle commands to build the project:
`gradle assembleDebug`

## Screenshot Tests
This project uses Roborazzi for screenshot tests. **Do not attempt to write binary images through text-editing tools**.
- To generate or update the baseline reference screenshots, run: `gradle :app:recordRoborazziDebug`
- To verify the current UI matches the baseline, run: `gradle :app:verifyRoborazziDebug`

Test assets are located under `app/src/test/screenshots/`.
