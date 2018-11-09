package com.simple.bot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.simple.bot.units.WeatherUnits;
import com.simple.bot.utils.JsonReader;
import com.simple.bot.utils.JsonUtil;
import com.simple.models.CurrentWeather;
import com.simple.models.Forecast;
import com.simple.models.WeatherCondition;


public class Bot extends TelegramLongPollingBot {

	private static final Logger logger = LoggerFactory.getLogger(Bot.class);
	private final String messagesBundleName = "weatherBot.messages";
	private String weatherApiKey = "";
	private String language = "en";
	private final String weatherApiUrl = "http://api.openweathermap.org/data/2.5/{endpoint}";
	private ResourceBundle messageBundle = ResourceBundle.getBundle(messagesBundleName, Locale.ENGLISH);
	private WeatherUnits units = WeatherUnits.metric;
	private String googleApiKey = "";
	final String baseUrl = "https://maps.googleapis.com/maps/api/geocode/json";
	private float latitude;
	private float longitude;
	private String city;

	/**
	 * Message handler
	 * 
	 * @param update
	 *            Has messages from user          
	 * @throws Exception
	 */
	@Override
	public void onUpdateReceived(Update update) {

		Message messages = update.getMessage();

		if (update.getMessage().hasLocation()) {
			latitude = messages.getLocation().getLatitude();
			longitude = messages.getLocation().getLongitude();
			try {
				setCity(reverseGeo(latitude, longitude));		
			} catch (JSONException | IOException e) {
				logger.error("Cannot get location data", e);
			}
		}
			if (messages != null && messages.hasText()) {
				String message_text = messages.getText();
				switch (message_text) {
				case "/start":
					sendMsg(messages, messageBundle.getString("start"));
					break;	
				case "/help":
					sendMsg(messages, messageBundle.getString("help"));
					break;
				case "/weather":
					sendCurrentWeather(messages);
					break;
				case "/forecast":
					sendForecast(messages);
					break;
				case "/location":
					sendLocationMsg(messages, "Press button", setButtons());
					break;
				default:
					sendMsg(messages, messageBundle.getString("unknown_command"));
				}
			}
		}
	
	/**
	 * Get reverse geolocation 
	 * 
	 * @param lat
	 *            Latitude coordinate of user location
	 * @param lng
	 *           Longitude coordinate of user location           
	 * @return cityName  
	 * 			Current city of the user location         
	 */
	private String reverseGeo(float lat, float lng) throws JSONException, IOException {
		String cityName ="";
		final String url = baseUrl + "?language=en&latlng=" + lat + "," + lng + "&key=" + googleApiKey;
		final JSONObject response = JsonReader.read(url);
		final JSONArray location = response.getJSONArray("results");
		final JSONObject formattedAddress = location.getJSONObject(0);
		JSONArray address_components = formattedAddress.getJSONArray("address_components");

		for (int i = 0; i < address_components.length(); i++) {
			JSONObject zero2 = address_components.getJSONObject(i);
			String long_name = zero2.getString("long_name");
			JSONArray mtypes = zero2.getJSONArray("types");
			String Type = mtypes.getString(0);
			if (Type.equalsIgnoreCase("locality")) {
				cityName = long_name;
			}
		}
		return cityName;
	}

	/**
	 * Send message to chat
	 * 
	 * @param messages
	 *            Updated messages
	 * @param s
	 *            String message to be send          
	 * @throws Exception
	 */
	private void sendMsg(Message messages, String s) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setChatId(messages.getChatId().toString());
		sendMessage.setReplyToMessageId(messages.getMessageId());
		sendMessage.setText(s);
		try {
			execute(sendMessage);
		} catch (TelegramApiException e) {
			logger.error("Cannot send message", e);
		}
	}

	/**
	 * Send message to chat to share location
	 * 
	 * @param messages
	 *             Updated messages
	 * @param s
	 *            Message to be send
	 * @param replyKeyboard
	 *            ReplyKeyboardMarkup
	 * @throws Exception                 
	 */
	private void sendLocationMsg(Message messages, String s, ReplyKeyboardMarkup replyKeyboard) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setChatId(messages.getChatId().toString());
		sendMessage.setReplyToMessageId(messages.getMessageId());
		sendMessage.setText(s);
		sendMessage.setReplyMarkup(replyKeyboard);
		try {
			execute(sendMessage);
		} catch (TelegramApiException e) {
			logger.error("Cannot send location message", e);
			sendMsg(messages, messageBundle.getString("location_get_error"));
		}
	}

	/**
	 * Returns bot name
	 * 
	 * @return Bot name
	 */
	public String getBotUsername() {
		return "Assistant";
	}

	/**
	 * Returns bot token
	 * 
	 * @return token
	 */
	@Override
	public String getBotToken() {
		return "";
	}

	/**
	 * Send request to chat to share location
	 * 
	 * @return replyKeyboard
	 */
	private ReplyKeyboardMarkup setButtons() {

		ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
		KeyboardButton locationButton = new KeyboardButton("Share location");
		locationButton.setRequestLocation(true);
		KeyboardRow row = new KeyboardRow();
		List<KeyboardRow> keyboard = new ArrayList<>();
		row.add(locationButton);
		keyboard.add(row);
		replyKeyboard.setKeyboard(keyboard);
		replyKeyboard.setResizeKeyboard(true);

		return replyKeyboard;
	}

	/**
	 * Send weather to chat
	 * 
	 * @param messages
	 *             Updated messages
	 * @throws Exception
	 */
	private void sendCurrentWeather(Message messages) {
		try {
			sendCurrentWeather(messages, city);
		} catch (Exception e) {
			logger.error("Weather error", e);
			sendMsg(messages, messageBundle.getString("weather_get_error"));
		}
	}

	/**
	 * Send weather in city to chat
	 * 
	 * @param messages
	 *             Updated messages
	 * @param city
	 *            City name
	 * @throws Exception
	 */
	private void sendCurrentWeather(Message messages, String city) {
		try {
			CurrentWeather currentWeather = getCurrentWeather(city);

			int temperature = (int) Math.round(currentWeather.getWeather().getTemperature());
			int humidity = currentWeather.getWeather().getHumidity();
			int pressure = (int) Math.round(currentWeather.getWeather().getPressure() * 0.75006375541921);
			String conditions = getConditions(currentWeather.getWeatherCondition());

			String weather = String.format(messageBundle.getString("current_weather_format"),
					currentWeather.getCityName(), temperature, conditions, humidity, pressure);

			sendMsg(messages, weather);
		} catch (Exception e) {
				logger.error("Weather error", e);
			sendMsg(messages, messageBundle.getString("weather_get_error"));
		}
	}

	/**
	 * Send forecast to chat
	 * 
	 * @param messages
	 *             Updated messages
	 * @throws Exception
	 */
	private void sendForecast(Message messages) {
		try {
			Forecast forecast = getForecast(city);

			List<String> forecastItems = new ArrayList<>();
			for (CurrentWeather weather : forecast.getList()) {
				int minTemp = (int) Math.round(weather.getWeather().getMinTemperature());
				int maxTemp = (int) Math.round(weather.getWeather().getMaxTemperature());
				String conditions = getConditions(weather.getWeatherCondition());
				String forecastItem = String.format(messageBundle.getString("forecast_item_format"),
						weather.getDateText(), minTemp, maxTemp, conditions);
				forecastItems.add(forecastItem);
			}

			String forecastStr = String.format(messageBundle.getString("forecast_format"), forecast.getCity().getName(),
					forecast.getCity().getCountry(), String.join("\n", forecastItems));

			sendMsg(messages, forecastStr);
		} catch (Exception e) {
			logger.error("Forecast error", e);
			sendMsg(messages, messageBundle.getString("forecast_get_error"));
		}
	}

	/**
	 * Get current weather
	 * 
	 * @param city
	 *            City to get
	 * @return Current weather
	 * @throws Exception
	 */
	private CurrentWeather getCurrentWeather(String city) throws Exception {
		try {
			JSONObject weatherObject = getWeatherObject("weather", city);
			CurrentWeather weather = JsonUtil.toObject(weatherObject, CurrentWeather.class);
			if (weather == null) {
				throw new Exception("Cannot parse weather");
			}
			return weather;
		} catch (Exception e) {
			logger.error("Cannot get weather data", e);
			throw e;
		}
	}

	/**
	 * Get 5 day forecast
	 * 
	 * @param city
	 *            City to get
	 * @return Weather forecast
	 * @throws Exception
	 */
	private Forecast getForecast(String city) throws Exception {
		try {
			JSONObject forecastObject = getWeatherObject("forecast", city);
			Forecast forecast = JsonUtil.toObject(forecastObject, Forecast.class);
			if (forecast == null) {
				throw new Exception("Cannot parse forecast");
			}
			return forecast;
		} catch (Exception e) {
			logger.error("Cannot get forecast", e);
			throw e;
		}
	}

	/**
	 * Get weather object
	 * 
	 * @param endpoint
	 *            API endpoint
	 * @param city
	 *            City to get
	 * @return Weather json object
	 */
	private JSONObject getWeatherObject(String endpoint, String city) throws Exception {
		HttpResponse<JsonNode> response = Unirest.get(weatherApiUrl).routeParam("endpoint", endpoint)
				.queryString("APPID", weatherApiKey).queryString("lang", language)
				.queryString("units", units.toString()).queryString("q", city).asJson();
		return response.getBody().getObject();
	}

	/**
	 * Collect conditions string
	 * 
	 * @param conditions
	 *            Conditions list
	 * @return Collected conditions
	 */
	private String getConditions(List<WeatherCondition> conditions) {
		return conditions.stream().map(WeatherCondition::getDescription).collect(Collectors.joining(", "));
	}

	public String getWeatherApiKey() {
		return weatherApiKey;
	}

	public String getWeatherApiUrl() {
		return weatherApiUrl;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
		messageBundle = ResourceBundle.getBundle(messagesBundleName, new Locale(language));
	}

	public WeatherUnits getUnits() {
		return units;
	}

	public void setUnits(WeatherUnits units) {
		this.units = units;
	}
	
	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}
	
}
