# ebayWeatherApp
Basic Weather App using OpenWeatherMap for application

USAGE:

Download ebayWeatherApp and import this Android project into your IDE.

Built using Android Studio 2.1.2 using gradle 2.10 for environmental reasons
(I currently cannot upgrade to the most recent versions to preserve active projects)
Because this is the case, I included the gradle files.

minSDKVersion:17
buildToolsVersion: 25.0.2

Location permissions are required to use app.

The biggest improvement I can see is adding in google's location services.  This is less of a problem on a device with more than just wifi connection but on the emmulator, if you have never used a location before, the app will be unable to see the last known location.

To correct this, try running google maps or a google search near you to ping the location.  After this, the Weather App should be able to get a last known location. I did not include Google services due to the app needing to remain simple.
