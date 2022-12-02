function getWeather(lat, lon) {
	var apiKey = $jsapi.context().injector.weatherApiKey;
	var response = $http.get("http://api.openweathermap.org/data/2.5/weather?APPID=${APPID}&units=${units}&lang=${lang}&lat=${lat}&lon=${lon}", {
            timeout: 10000,
            query:{
                APPID: apiKey,
                units: "metric",
                lang: "ru",
                lat: lat,
                lon: lon
            }
        });

	if (!response.isOk || !response.data) {
		return false;
	}

	var weather = {};

	weather.temp = response.data.main.temp;
	weather.feelslike = response.data.main.feels_like;
	weather.description = response.data.weather[0].description;

	return weather;
}


