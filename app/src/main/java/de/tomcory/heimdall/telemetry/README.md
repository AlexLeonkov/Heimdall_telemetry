The TelemetryService is an integral part of the Heimdall designed to automatically collect, anonymize, and export telemetry data to a server.

Features

Automatic Data Collection: Collects data on apps, requests, responses, and connections in real-time.
Anonymization: Offers various levels of data anonymization to protect user privacy, including options for essential data only, inclusion of headers, and content.
Persistent Export: Automatically exports the collected and processed data to a predefined server endpoint.
Customizable Anonymization: Users can adjust the anonymization level via the app's preferences.

Setup and adjustments

The service starts automatically when the application creates it, collecting telemetry data in the background. Data collection preferences can be adjusted in the app's settings, allowing users to control the anonymization level.

To make sure that the framework is able to send the data to the server make sure that right IP adress (baseUrl) is set up in Retrofit.Builder()

To adjust new modules new module should be added to AnonymizationFlags object. Additionally logic for new modules handling should be added to anonymization function: anonymizeRequest(); anonymizeResponse();
anonymizeConnection()

Also UI TelemetryScreen.kt should be adjusted to inclule switcher for enabling new module.
