////////////////////////////////MainActivity.java//////////////////////////////////
//
// Title:    Weather App
// Purpose:  Atom Finance Android Engineer Position
//
// Author:   Colin Guest
// Email:    colinguest@me.com
// Phone:    +1(414)614-7955
//
///////////////////////////////////////////////////////////////////////////////////
package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    //all of the views on the weather app screen
    private TextView currentCity;
    private TextView currentTemperature;
    private TextView currentWeatherType;
    private EditText inputZipCode;
    private TextView sunrise, sunset;
    private TextView day1Weather, day2Weather, day3Weather, day4Weather, day5Weather, day6Weather;
    private TextView day1Day, day2Day, day3Day, day4Day, day5Day, day6Day;
    private TextView day1High, day2High, day3High, day4High, day5High, day6High;
    private TextView day1Low, day2Low, day3Low, day4Low, day5Low, day6Low;
    public CheckBox toggleFahrenheit;

    //Miscellaneous Strings
    private String cityName, currentTemp;
    private String day1TempHigh, day6TempHigh;
    private String day1TempLow, day6TempLow;

    //ArrayLists for gathering weather data systematically
    private ArrayList<JSONObject> listIndexList = new ArrayList<>(40);
    private ArrayList<JSONArray> weatherArrList = new ArrayList<>(40);
    private ArrayList<JSONObject> objectWeatherList = new ArrayList<>(40);
    private ArrayList<String> rawWeatherDescriptionList = new ArrayList<>(40);
    private ArrayList<String> cleanWeatherDescriptionList = new ArrayList<>(40);
    private ArrayList<JSONObject> mainList = new ArrayList<>(40);
    private ArrayList<Double> tempMaxList = new ArrayList<>(40);
    private ArrayList<Double> tempMinList = new ArrayList<>(40);

    //URL's for current weather (personal API) and five-day forecast (given by Atom)
    private final String fiveDayWeatherURL = "http://api.openweathermap.org/data/2.5/forecast?id=524901&APPID=c1d0fb40222e923a2ead672b6ea4be52&zip=";
    private final String currentWeatherURL1 = "https://api.openweathermap.org/data/2.5/weather?zip=";
    private final String currentWeatherURL2 = ",us&appid=2e328e3f43b188420a0ae5007dad01d7";
    private final String[] DAYS_OF_THE_WEEK = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    //String for initial zip code (Brooklyn used)
    private String zipCode = "11201";

    //Used for updating API every minute (uses clock tick on device)
    private BroadcastReceiver minuteUpdateReceiver;

    //When app is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set current weather views
        currentCity = findViewById(R.id.current_city);
        currentTemperature = findViewById(R.id.current_temperature);
        currentWeatherType = findViewById(R.id.current_weather);

        //sunrise/sunset Views
        sunrise = findViewById(R.id.sunriseView);
        sunset = findViewById(R.id.sunsetView);

        //set 6 day forecast Views
        View Day1 = findViewById(R.id.day1);
        View Day2 = findViewById(R.id.day2);
        View Day3 = findViewById(R.id.day3);
        View Day4 = findViewById(R.id.day4);
        View Day5 = findViewById(R.id.day5);
        View Day6 = findViewById(R.id.day6);

        //days
        day1Day = Day1.findViewById(R.id.dayOfWeek);
        day2Day = Day2.findViewById(R.id.dayOfWeek);
        day3Day = Day3.findViewById(R.id.dayOfWeek);
        day4Day = Day4.findViewById(R.id.dayOfWeek);
        day5Day = Day5.findViewById(R.id.dayOfWeek);
        day6Day = Day6.findViewById(R.id.dayOfWeek);

        //daily highs
        day1High = Day1.findViewById(R.id.dailyHigh);
        day2High = Day2.findViewById(R.id.dailyHigh);
        day3High = Day3.findViewById(R.id.dailyHigh);
        day4High = Day4.findViewById(R.id.dailyHigh);
        day5High = Day5.findViewById(R.id.dailyHigh);
        day6High = Day6.findViewById(R.id.dailyHigh);

        //daily lows
        day1Low = Day1.findViewById(R.id.dailyLow);
        day2Low = Day2.findViewById(R.id.dailyLow);
        day3Low = Day3.findViewById(R.id.dailyLow);
        day4Low = Day4.findViewById(R.id.dailyLow);
        day5Low = Day5.findViewById(R.id.dailyLow);
        day6Low = Day6.findViewById(R.id.dailyLow);

        //weather descriptions
        day1Weather = Day1.findViewById(R.id.weatherType);
        day2Weather = Day2.findViewById(R.id.weatherType);
        day3Weather = Day3.findViewById(R.id.weatherType);
        day4Weather = Day4.findViewById(R.id.weatherType);
        day5Weather = Day5.findViewById(R.id.weatherType);
        day6Weather = Day6.findViewById(R.id.weatherType);

        //set action views
        inputZipCode = findViewById(R.id.zipCode);
        toggleFahrenheit = findViewById(R.id.fahrenheit);

        //set text for zipcode (could modify this to get zip code from location and set to zipcode on create)
        inputZipCode.setText(zipCode);
        //call searchZip so that the weather is shown upon opening
        searchZip(inputZipCode);
        }

        //Start the UI updater
        @Override
        protected void onResume() {
            super.onResume();
            minuteUpdater();
        }

        //Pause the UI updater
        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(minuteUpdateReceiver);
        }

        //search method that queries the API's to get the weather data
        public void searchZip(View view) {
            //initialize temporary URL's for OpenWeather API inputs later
            String currentTempURL;
            String fiveDayForecastURL;

            //retrieve the input zip code from the EditText view
            zipCode = inputZipCode.getText().toString().trim();
            //proper zip code pattern
            String regex = "^[0-9]{5}(?:-[0-9]{4})?$";
            //save pattern for next comparison
            Pattern pattern = Pattern.compile(regex);
            //save comparison as a Matcher object
            Matcher matcher = pattern.matcher(zipCode);

            //if invalid zip code is input, send Error to EditText's hint
            if (!(matcher.matches())) {
                inputZipCode.setHint("Valid Zips Only");
            } else {
                //create the OpenWeatherAPI search based off the validated zipCode
                currentTempURL = currentWeatherURL1 + zipCode + currentWeatherURL2;
                fiveDayForecastURL = fiveDayWeatherURL + zipCode;

                //current weather request
                StringRequest currentWeatherStringRequest = new StringRequest(Request.Method.POST, currentTempURL, response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);

                        //get the current weather description from jsonResponse
                        JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                        JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                        String description = jsonObjectWeather.getString("description");

                        //format description to upper case words
                        description = description.substring(0, 1).toUpperCase() + description.substring(1).toLowerCase();

                        //Set text for current weather
                        currentWeatherType.setText(description);
                        day1Weather.setText(description);

                        //get current temperature
                        JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                        double temp = jsonObjectMain.getDouble("temp") - 273.15;
                        if (toggleFahrenheit.isChecked()) {
                            temp = getFahrenheit(temp);
                        }
                        currentTemp = Integer.toString((int) Math.round(temp));
                        currentTemperature.setText(currentTemp);

                        //get today's high
                        double tempHigh = jsonObjectMain.getDouble("temp_max") - 273.15;
                        if (toggleFahrenheit.isChecked()) {
                            tempHigh = getFahrenheit(tempHigh);
                        }
                        day1TempHigh = Integer.toString((int) Math.round(tempHigh));
                        day1High.setText(day1TempHigh);

                        //get today's low
                        double tempLow = jsonObjectMain.getDouble("temp_min") - 273.15;
                        if (toggleFahrenheit.isChecked()) {
                            tempLow = getFahrenheit(tempLow);
                        }
                        day1TempLow = Integer.toString((int) Math.round(tempLow));
                        day1Low.setText(day1TempLow);

                        //get and set city name from jsonResponse
                        cityName = jsonResponse.getString("name");
                        currentCity.setText(cityName);

                        //get sunrise, sunset, and set days of the week
                        JSONObject jsonSys = jsonResponse.getJSONObject("sys");
                        long sunRise = (jsonSys.getLong("sunrise")) * 1000;
                        long sunSet = (jsonSys.getLong("sunset")) * 1000;
                        Date sunRiseDate = new Date(sunRise);
                        Date sunSetDate = new Date(sunSet);
                        String [] pickSunriseTime = sunRiseDate.toString().split(" ", 6);
                        String [] pickSunsetTime = sunSetDate.toString().split(" ", 6);

                        //String that gives you sunrise and sunset with timezone of the person
                        String sunriseTime = "Sunrise: " + pickSunriseTime[3].substring(0, 5) + "AM " + pickSunriseTime[4];
                        String sunsetTime = "Sunset: " + sunsetModifier(Integer.parseInt(pickSunsetTime[3].substring(0, 2))) + pickSunsetTime[3].substring(2, 5) + "PM " + pickSunsetTime[4];
                        String currentDay = pickSunriseTime[0];
                        setDays(currentDay);
                        sunrise.setText(sunriseTime);
                        sunset.setText(sunsetTime);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }, error -> Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_SHORT).show());
                RequestQueue currentWeatherRequestQueue = Volley.newRequestQueue(getApplicationContext());
                currentWeatherRequestQueue.add(currentWeatherStringRequest);

                //5 Day Forecast weather request
                StringRequest fiveDayForecastStringRequest = new StringRequest(Request.Method.POST, fiveDayForecastURL, response -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);

                        //get list values for current temperature highs lows and weather descriptions
                        JSONArray jsonList = jsonResponse.getJSONArray("list");
                        JSONObject listIndex0 = jsonList.getJSONObject(0);

                        //compare today's date with that of the zeroth index of five day forecast
                        Date currentTime = Calendar.getInstance().getTime();
                        long zerothDateLong = (listIndex0.getLong("dt")) * 1000;
                        Date zerothDate = new Date(zerothDateLong);

                        //get the day of the week from the device and the five day forecast
                        String [] todayTime = currentTime.toString().split(" ", 6);
                        String [] zerothTime = zerothDate.toString().split(" ", 6);

                        //get the zeroth time index to see which three-hour window you start with
                        String zerothIndexTime = getZerothIndexTime(listIndex0);

                        //compare the days of the week to see if they match
                        if (zerothTime[0].equals(todayTime[0])) { //if zeroth index for five day forecast given on same day as today
                            getSameDaySixDayWeatherForecast(jsonList, zerothIndexTime);
                        } else { // zeroth index for five day forecast given starting tomorrow (usually happens during nighttime queries)
                            getDifferentDaySixDayWeatherForecast(jsonList, zerothIndexTime);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }, error -> Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_SHORT).show());
                RequestQueue fiveDayWeatherRequestQueue = Volley.newRequestQueue(getApplicationContext());
                fiveDayWeatherRequestQueue.add(fiveDayForecastStringRequest);
            }
        }

        /**
         * set the days of the week depending on the current day
         *
         * @param currentDay - is the current day of the week (Mon-Sun)
         */
         public void setDays(String currentDay) {
            if(currentDay.equals(DAYS_OF_THE_WEEK[0])) {
                day1Day.setText(DAYS_OF_THE_WEEK[0]);
                day2Day.setText(DAYS_OF_THE_WEEK[1]);
                day3Day.setText(DAYS_OF_THE_WEEK[2]);
                day4Day.setText(DAYS_OF_THE_WEEK[3]);
                day5Day.setText(DAYS_OF_THE_WEEK[4]);
                day6Day.setText(DAYS_OF_THE_WEEK[5]);
                }
            if(currentDay.equals(DAYS_OF_THE_WEEK[1])) {
                day1Day.setText(DAYS_OF_THE_WEEK[1]);
                day2Day.setText(DAYS_OF_THE_WEEK[2]);
                day3Day.setText(DAYS_OF_THE_WEEK[3]);
                day4Day.setText(DAYS_OF_THE_WEEK[4]);
                day5Day.setText(DAYS_OF_THE_WEEK[5]);
                day6Day.setText(DAYS_OF_THE_WEEK[6]);
            }
            if(currentDay.equals(DAYS_OF_THE_WEEK[2])) {
                day1Day.setText(DAYS_OF_THE_WEEK[2]);
                day2Day.setText(DAYS_OF_THE_WEEK[3]);
                day3Day.setText(DAYS_OF_THE_WEEK[4]);
                day4Day.setText(DAYS_OF_THE_WEEK[5]);
                day5Day.setText(DAYS_OF_THE_WEEK[6]);
                day6Day.setText(DAYS_OF_THE_WEEK[0]);
            }
            if(currentDay.equals(DAYS_OF_THE_WEEK[3])) {
                day1Day.setText(DAYS_OF_THE_WEEK[3]);
                day2Day.setText(DAYS_OF_THE_WEEK[4]);
                day3Day.setText(DAYS_OF_THE_WEEK[5]);
                day4Day.setText(DAYS_OF_THE_WEEK[6]);
                day5Day.setText(DAYS_OF_THE_WEEK[0]);
                day6Day.setText(DAYS_OF_THE_WEEK[1]);
            }
            if(currentDay.equals(DAYS_OF_THE_WEEK[4])) {
                day1Day.setText(DAYS_OF_THE_WEEK[4]);
                day2Day.setText(DAYS_OF_THE_WEEK[5]);
                day3Day.setText(DAYS_OF_THE_WEEK[6]);
                day4Day.setText(DAYS_OF_THE_WEEK[0]);
                day5Day.setText(DAYS_OF_THE_WEEK[1]);
                day6Day.setText(DAYS_OF_THE_WEEK[2]);
            }
            if(currentDay.equals(DAYS_OF_THE_WEEK[5])) {
                day1Day.setText(DAYS_OF_THE_WEEK[5]);
                day2Day.setText(DAYS_OF_THE_WEEK[6]);
                day3Day.setText(DAYS_OF_THE_WEEK[0]);
                day4Day.setText(DAYS_OF_THE_WEEK[1]);
                day5Day.setText(DAYS_OF_THE_WEEK[2]);
                day6Day.setText(DAYS_OF_THE_WEEK[3]);
            }
            if(currentDay.equals(DAYS_OF_THE_WEEK[6])) {
                day1Day.setText(DAYS_OF_THE_WEEK[6]);
                day2Day.setText(DAYS_OF_THE_WEEK[0]);
                day3Day.setText(DAYS_OF_THE_WEEK[1]);
                day4Day.setText(DAYS_OF_THE_WEEK[2]);
                day5Day.setText(DAYS_OF_THE_WEEK[3]);
                day6Day.setText(DAYS_OF_THE_WEEK[4]);
            }
        }

        /**
         * give the zeroth time index of five-day forecast to see where to start calculating weather temps and conditions
         *
         * @param listIndex0 - the object at the zeroth position in the beginning list array
         * @return the time entry value of the zeroth index
         * @throws JSONException
         */
        public String getZerothIndexTime(JSONObject listIndex0) throws JSONException {
            String str = listIndex0.getString("dt_txt");
            String [] ZerothTimeEntry = str.split(" ", 2);
            return ZerothTimeEntry[1].substring(0,2);
        }

        /**
         * set the weather conditions and highs and lows based on which time index is given first
         * only called when today's Day of the Week is the same as that of the zeroth index of
         * five day weather forecast
         * USUALLY only true of a nighttime query when the first three hour weather forecast is tomorrow,
         * but wrote the whole method for all scenarios
         *
         * @param jsonList - the list holding all temperature and weather objects, arrays and data
         * @param zerothIndexTime - which index time is the first given (only 8 possible values)
         *
         */
        public void getSameDaySixDayWeatherForecast(JSONArray jsonList, String zerothIndexTime) throws JSONException {

            //clear all array lists for new entries
            listIndexList.clear();
            weatherArrList.clear();
            objectWeatherList.clear();
            rawWeatherDescriptionList.clear();
            cleanWeatherDescriptionList.clear();
            mainList.clear();
            tempMaxList.clear();
            tempMinList.clear();

            //declare local variables
            double temporaryMin6;
            double temporaryMax6;

            for (int i=0; i < 40; i++){
                //add all list indexes to arraylists
                listIndexList.add(jsonList.getJSONObject(i));

                //add all weather descriptions to array lists
                weatherArrList.add(listIndexList.get(i).getJSONArray("weather"));
                objectWeatherList.add(weatherArrList.get(i).getJSONObject(0));
                rawWeatherDescriptionList.add(objectWeatherList.get(i).getString("description"));
                cleanWeatherDescriptionList.add(rawWeatherDescriptionList.get(i).substring(0, 1).toUpperCase() + rawWeatherDescriptionList.get(i).substring(1).toLowerCase());

                //add all highs and lows to ArrayLists
                mainList.add(listIndexList.get(i).getJSONObject("main"));
                tempMaxList.add(mainList.get(i).getDouble("temp_max")- 273.15); //add all temp maxes to ArrayLists in celsius form
                tempMinList.add(mainList.get(i).getDouble("temp_min")- 273.15); //add all temp mins to ArrayLists in celsius form
            }
            if (zerothIndexTime.equals("00")) {
                //find each day's weather at noon
                //day 1
                day1Weather.setText(cleanWeatherDescriptionList.get(4));
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(12));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(20));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(28));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(36));
                //day 6 (Inaccurate due to 40 three-hour forecasts)
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 1
                day1Low.setText(getDayLow(0));
                //day 2
                day2Low.setText(getDayLow(8));
                //day 3
                day3Low.setText(getDayLow(16));
                //day 4
                day4Low.setText(getDayLow(24));
                //day 5
                day5Low.setText(getDayLow(32));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(39);
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 1
                day1High.setText(getDayHigh(0));
                //day 2
                day2High.setText(getDayHigh(8));
                //day 3
                day3High.setText(getDayHigh(16));
                //day 4
                day4High.setText(getDayHigh(24));
                //day 5
                day5High.setText(getDayHigh(32));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(39);
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("03")) {
                //find each day's weather at noon
                //day 1
                day1Weather.setText(cleanWeatherDescriptionList.get(3));
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(11));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(19));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(27));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(35));
                //day 6 (Inaccurate due to 40 three-hour forecasts)
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 1
                day1Low.setText(getDayLow(0));
                //day 2
                day2Low.setText(getDayLow(7));
                //day 3
                day3Low.setText(getDayLow(15));
                //day 4
                day4Low.setText(getDayLow(23));
                //day 5
                day5Low.setText(getDayLow(31));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(39);
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 1
                day1High.setText(getDayHigh(0));
                //day 2
                day2High.setText(getDayHigh(7));
                //day 3
                day3High.setText(getDayHigh(15));
                //day 4
                day4High.setText(getDayHigh(23));
                //day 5
                day5High.setText(getDayHigh(31));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(39);
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("06")) {
                //find each day's weather at noon
                //day 1
                day1Weather.setText(cleanWeatherDescriptionList.get(2));
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(10));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(18));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(26));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(34));
                //day 6 (Inaccurate due to 40 three-hour forecasts)
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 1
                day1Low.setText(getDayLow(0));
                //day 2
                day2Low.setText(getDayLow(6));
                //day 3
                day3Low.setText(getDayLow(14));
                //day 4
                day4Low.setText(getDayLow(22));
                //day 5
                day5Low.setText(getDayLow(30));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(38);
                for (int i = 38; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 1
                day1High.setText(getDayHigh(0));
                //day 2
                day2High.setText(getDayHigh(6));
                //day 3
                day3High.setText(getDayHigh(14));
                //day 4
                day4High.setText(getDayHigh(22));
                //day 5
                day5High.setText(getDayHigh(30));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(38);
                for (int i = 38; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("09")) {
                //find each day's weather at noon
                //day 1
                day1Weather.setText(cleanWeatherDescriptionList.get(1));
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(9));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(17));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(25));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(33));
                //day 6 (Inaccurate due to 40 three-hour forecasts)
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 1
                day1Low.setText(getDayLow(0));
                //day 2
                day2Low.setText(getDayLow(5));
                //day 3
                day3Low.setText(getDayLow(13));
                //day 4
                day4Low.setText(getDayLow(21));
                //day 5
                day5Low.setText(getDayLow(29));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(37);
                for (int i = 37; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 1
                day1High.setText(getDayHigh(0));
                //day 2
                day2High.setText(getDayHigh(5));
                //day 3
                day3High.setText(getDayHigh(13));
                //day 4
                day4High.setText(getDayHigh(21));
                //day 5
                day5High.setText(getDayHigh(29));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(37);
                for (int i = 37; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("12")) {
                //find each day's weather at noon
                //day 1
                day1Weather.setText(cleanWeatherDescriptionList.get(0));
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(8));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(16));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(24));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(32));
                //day 6 (Inaccurate due to 40 three-hour forecasts)
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 1
                day1Low.setText(getDayLow(0));
                //day 2
                day2Low.setText(getDayLow(4));
                //day 3
                day3Low.setText(getDayLow(12));
                //day 4
                day4Low.setText(getDayLow(20));
                //day 5
                day5Low.setText(getDayLow(28));
                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(36);
                for (int i = 36; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 1
                day1High.setText(getDayHigh(0));
                //day 2
                day2High.setText(getDayHigh(4));
                //day 3
                day3High.setText(getDayHigh(12));
                //day 4
                day4High.setText(getDayHigh(20));
                //day 5
                day5High.setText(getDayHigh(28));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(36);
                for (int i = 36; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("15")) {
                //find each day's weather at noon
                //start with day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(7));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(15));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(23));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(31));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(39));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(3));
                //day 3
                day3Low.setText(getDayLow(11));
                //day 4
                day4Low.setText(getDayLow(19));
                //day 5
                day5Low.setText(getDayLow(27));

                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(35);
                for (int i = 35; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(3));
                //day 3
                day3High.setText(getDayHigh(11));
                //day 4
                day4High.setText(getDayHigh(19));
                //day 5
                day5High.setText(getDayHigh(27));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(35);
                for (int i = 35; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("18")) {
                //find each day's weather at noon
                //start with day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(6));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(14));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(22));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(30));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(38));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(2));
                //day 3
                day3Low.setText(getDayLow(10));
                //day 4
                day4Low.setText(getDayLow(18));
                //day 5
                day5Low.setText(getDayLow(26));

                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(34);
                for (int i = 34; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(2));
                //day 3
                day3High.setText(getDayHigh(10));
                //day 4
                day4High.setText(getDayHigh(18));
                //day 5
                day5High.setText(getDayHigh(26));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(34);
                for (int i = 35; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
            if (zerothIndexTime.equals("21")) {
                //find each day's weather at noon
                //start with day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(5));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(13));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(21));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(29));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(37));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(1));
                //day 3
                day3Low.setText(getDayLow(9));
                //day 4
                day4Low.setText(getDayLow(17));
                //day 5
                day5Low.setText(getDayLow(25));

                //day 6 --> cannot use getDayLow method(), because we reach end of list
                temporaryMin6 = tempMinList.get(33);
                for (int i = 33; i < 40; i++) {
                    if(tempMinList.get(i) < temporaryMin6) {
                        temporaryMin6 = tempMinList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMin6 = getFahrenheit(temporaryMin6);
                }
                day6TempLow = Integer.toString((int) Math.round(temporaryMin6));
                day6Low.setText(day6TempLow);

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(1));
                //day 3
                day3High.setText(getDayHigh(9));
                //day 4
                day4High.setText(getDayHigh(17));
                //day 5
                day5High.setText(getDayHigh(25));
                //day 6 --> cannot use getDayHigh method(), because we reach end of list
                temporaryMax6 = tempMaxList.get(33);
                for (int i = 35; i < 40; i++) {
                    if(tempMaxList.get(i) > temporaryMax6) {
                        temporaryMax6 = tempMaxList.get(i);
                    }
                }
                if (toggleFahrenheit.isChecked()) {
                    temporaryMax6 = getFahrenheit(temporaryMax6);
                }
                day6TempHigh = Integer.toString((int) Math.round(temporaryMax6));
                day6High.setText(day6TempHigh);
            }
        }

        /**
         * set the weather conditions and highs and lows based on which time index is given first
         * only called when today's Day of the Week differs from that of the zeroth index of
         * five day weather forecast
         * USUALLY only true of a nighttime query when the first three hour weather forecast is tomorrow,
         * but wrote the whole method for all scenarios
         *
         * @param jsonList - the list holding all temperature and weather objects, arrays and data
         * @param zerothIndexTime - which index time is the first given (only 8 possible values)
         *
         */
        public void getDifferentDaySixDayWeatherForecast(JSONArray jsonList, String zerothIndexTime) throws JSONException {
            //clear all array lists for new entries
            listIndexList.clear();
            weatherArrList.clear();
            objectWeatherList.clear();
            rawWeatherDescriptionList.clear();
            cleanWeatherDescriptionList.clear();
            mainList.clear();
            tempMaxList.clear();
            tempMinList.clear();

            for (int i=0; i < 40; i++){
                //add all list indexes to arraylists
                listIndexList.add(jsonList.getJSONObject(i));
                //add all weather descriptions to array lists
                weatherArrList.add(listIndexList.get(i).getJSONArray("weather"));
                objectWeatherList.add(weatherArrList.get(i).getJSONObject(0));
                rawWeatherDescriptionList.add(objectWeatherList.get(i).getString("description"));
                cleanWeatherDescriptionList.add(rawWeatherDescriptionList.get(i).substring(0, 1).toUpperCase() + rawWeatherDescriptionList.get(i).substring(1).toLowerCase());

                //add all highs and lows to ArrayLists
                mainList.add(listIndexList.get(i).getJSONObject("main"));
                tempMaxList.add(mainList.get(i).getDouble("temp_max")- 273.15); //add all temp maxes to ArrayLists in celsius form
                tempMinList.add(mainList.get(i).getDouble("temp_min")- 273.15); //add all temp mins to ArrayLists in celsius form
               }
            if (zerothIndexTime.equals("00")) {
                //find each day's weather at noon
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(4));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(12));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(20));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(28));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(36));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(8));
                //day 4
                day4Low.setText(getDayLow(16));
                //day 5
                day5Low.setText(getDayLow(24));
                //day 6
                day6Low.setText(getDayLow(32));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(8));
                //day 4
                day4High.setText(getDayHigh(16));
                //day 5
                day5High.setText(getDayHigh(24));
                //day 6
                day6High.setText(getDayHigh(32));
            }
            if (zerothIndexTime.equals("03")) {
                //find each day's weather at noon
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(3));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(11));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(19));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(27));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(35));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(7));
                //day 4
                day4Low.setText(getDayLow(15));
                //day 5
                day5Low.setText(getDayLow(23));
                //day 6
                day6Low.setText(getDayLow(31));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(7));
                //day 4
                day4High.setText(getDayHigh(15));
                //day 5
                day5High.setText(getDayHigh(23));
                //day 6
                day6High.setText(getDayHigh(31));
            }
            if (zerothIndexTime.equals("06")) {
                //find each day's weather at noon
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(2));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(10));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(18));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(26));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(34));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(6));
                //day 4
                day4Low.setText(getDayLow(14));
                //day 5
                day5Low.setText(getDayLow(22));
                //day 6
                day6Low.setText(getDayLow(30));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(6));
                //day 4
                day4High.setText(getDayHigh(14));
                //day 5
                day5High.setText(getDayHigh(22));
                //day 6
                day6High.setText(getDayHigh(30));
            }
            if (zerothIndexTime.equals("09")) {
                //find each day's weather at noon
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(1));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(9));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(17));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(25));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(33));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(5));
                //day 4
                day4Low.setText(getDayLow(13));
                //day 5
                day5Low.setText(getDayLow(21));
                //day 6
                day6Low.setText(getDayLow(29));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(5));
                //day 4
                day4High.setText(getDayHigh(13));
                //day 5
                day5High.setText(getDayHigh(21));
                //day 6
                day6High.setText(getDayHigh(29));
            }
            if (zerothIndexTime.equals("12")) {
                //find each day's weather at noon
                //day 2
                day2Weather.setText(cleanWeatherDescriptionList.get(0));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(8));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(16));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(24));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(32));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(4));
                //day 4
                day4Low.setText(getDayLow(12));
                //day 5
                day5Low.setText(getDayLow(20));
                //day 6
                day6Low.setText(getDayLow(28));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(4));
                //day 4
                day4High.setText(getDayHigh(12));
                //day 5
                day5High.setText(getDayHigh(20));
                //day 6
                day6High.setText(getDayHigh(28));
            }
            if (zerothIndexTime.equals("15")) {
                //find each day's weather at noon
                //day 2 (inaccurate but unlikely outcome)
                day2Weather.setText(cleanWeatherDescriptionList.get(0));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(7));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(15));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(23));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(31));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(3));
                //day 4
                day4Low.setText(getDayLow(11));
                //day 5
                day5Low.setText(getDayLow(19));
                //day 6
                day6Low.setText(getDayLow(27));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(3));
                //day 4
                day4High.setText(getDayHigh(11));
                //day 5
                day5High.setText(getDayHigh(19));
                //day 6
                day6High.setText(getDayHigh(27));
            }
            if (zerothIndexTime.equals("18")) {
                //find each day's weather at noon
                //day 2 (inaccurate but unlikely outcome)
                day2Weather.setText(cleanWeatherDescriptionList.get(0));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(6));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(14));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(22));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(30));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(2));
                //day 4
                day4Low.setText(getDayLow(10));
                //day 5
                day5Low.setText(getDayLow(18));
                //day 6
                day6Low.setText(getDayLow(26));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(2));
                //day 4
                day4High.setText(getDayHigh(10));
                //day 5
                day5High.setText(getDayHigh(18));
                //day 6
                day6High.setText(getDayHigh(26));
            }
            if (zerothIndexTime.equals("21")) {
                //find each day's weather at noon
                //day 2 (inaccurate but unlikely outcome)
                day2Weather.setText(cleanWeatherDescriptionList.get(0));
                //day 3
                day3Weather.setText(cleanWeatherDescriptionList.get(5));
                //day 4
                day4Weather.setText(cleanWeatherDescriptionList.get(13));
                //day 5
                day5Weather.setText(cleanWeatherDescriptionList.get(21));
                //day 6
                day6Weather.setText(cleanWeatherDescriptionList.get(29));

                //find lowest low for each day
                //day 2
                day2Low.setText(getDayLow(0));
                //day 3
                day3Low.setText(getDayLow(1));
                //day 4
                day4Low.setText(getDayLow(9));
                //day 5
                day5Low.setText(getDayLow(17));
                //day 6
                day6Low.setText(getDayLow(25));

                //find highest high for each day
                //day 2
                day2High.setText(getDayHigh(0));
                //day 3
                day3High.setText(getDayHigh(1));
                //day 4
                day4High.setText(getDayHigh(9));
                //day 5
                day5High.setText(getDayHigh(17));
                //day 6
                day6High.setText(getDayHigh(25));
            }
        }

        /**
         * gives the lowest temperature for that day (8 queries to sift through)
         *
         * @param startIndex - starting index of the temp. query
         *
         * @return the low for that day
         *
         */
        public String getDayLow(int startIndex) {
            String low;
            double tempMin = tempMinList.get(startIndex);
            for (int i = startIndex; i < (startIndex + 8); i++) {
                if(tempMinList.get(i) < tempMin) {
                    tempMin = tempMinList.get(i);
                }
            }

            //presents the view in fahrenheit or celsius depending on the toggleFahrenheit check box
            if(toggleFahrenheit.isChecked()) {
                low = Integer.toString((int)Math.round(getFahrenheit(tempMin)));
            } else {
                low = Integer.toString((int) Math.round(tempMin));
            }
            return low;
        }

        /**
         * gives the highest temperature for that day (8 queries to sift through)
         *
         * @param startIndex - starting index of the temp. query
         *
         * @return the high for that day
         *
         */
        public String getDayHigh(int startIndex) {
            String high;
            double tempHigh = tempMaxList.get(startIndex);
            for (int i = startIndex; i < (startIndex + 8); i++) {
                if(tempMinList.get(i) > tempHigh) {
                    tempHigh = tempMinList.get(i);
                }
            }
            //presents the view in fahrenheit or celsius depending on the toggleFahrenheit check box
            if(toggleFahrenheit.isChecked()) {
                high = Integer.toString((int)Math.round(getFahrenheit(tempHigh)));
            } else {
                high = Integer.toString((int) Math.round(tempHigh));
            }
            return high;
        }

        /**
         * switch from C to F
         *
         * @param celsius - temp in celsius
         *
         * @return temp in fahrenheit
         *
         */
        public double getFahrenheit(double celsius) {
            if (toggleFahrenheit.isChecked()) {
                return ((celsius * 1.8) + 32);
            } else {
                return celsius;
            }
        }

        /**
         * switch from F to C
         *
         * @param fahr - temp in fahrenheit
         *
         * @return temp in celsius
         *
         */
        public double getCelsius(double fahr) {
            if (!(toggleFahrenheit.isChecked())) {
                return ((fahr - 32)/1.8);
            } else {
                return fahr;
            }
        }

        // changes the temperatures displayed from F to C or C to F,
        // depending on the toggleFahrenheit's status
        public void fahrenheitSwitch(View view){
            if (toggleFahrenheit.isChecked()) {
                double currentTemp = Double.parseDouble(currentTemperature.getText().toString());
                currentTemperature.setText(Integer.toString((int)Math.round(getFahrenheit(currentTemp))));
                //change highs
                double high1 = Double.parseDouble(day1High.getText().toString());
                day1High.setText(Integer.toString((int)Math.round(getFahrenheit(high1))));
                double high2 = Double.parseDouble(day2High.getText().toString());
                day2High.setText(Integer.toString((int)Math.round(getFahrenheit(high2))));
                double high3 = Double.parseDouble(day3High.getText().toString());
                day3High.setText(Integer.toString((int)Math.round(getFahrenheit(high3))));
                double high4 = Double.parseDouble(day4High.getText().toString());
                day4High.setText(Integer.toString((int)Math.round(getFahrenheit(high4))));
                double high5 = Double.parseDouble(day5High.getText().toString());
                day5High.setText(Integer.toString((int)Math.round(getFahrenheit(high5))));
                double high6 = Double.parseDouble(day6High.getText().toString());
                day6High.setText(Integer.toString((int)Math.round(getFahrenheit(high6))));
                //change lows
                double low1 = Double.parseDouble(day1Low.getText().toString());
                day1Low.setText(Integer.toString((int)Math.round(getFahrenheit(low1))));
                double low2 = Double.parseDouble(day2Low.getText().toString());
                day2Low.setText(Integer.toString((int)Math.round(getFahrenheit(low2))));
                double low3 = Double.parseDouble(day3Low.getText().toString());
                day3Low.setText(Integer.toString((int)Math.round(getFahrenheit(low3))));
                double low4 = Double.parseDouble(day4Low.getText().toString());
                day4Low.setText(Integer.toString((int)Math.round(getFahrenheit(low4))));
                double low5 = Double.parseDouble(day5Low.getText().toString());
                day5Low.setText(Integer.toString((int)Math.round(getFahrenheit(low5))));
                double low6 = Double.parseDouble(day6Low.getText().toString());
                day6Low.setText(Integer.toString((int)Math.round(getFahrenheit(low6))));
            } if(!(toggleFahrenheit.isChecked())) {
                double currentTemp = Double.parseDouble(currentTemperature.getText().toString());
                currentTemperature.setText(Integer.toString((int)Math.round(getCelsius(currentTemp))));
                //change highs
                double high1 = Double.parseDouble(day1High.getText().toString());
                day1High.setText(Integer.toString((int)Math.round(getCelsius(high1))));
                double high2 = Double.parseDouble(day2High.getText().toString());
                day2High.setText(Integer.toString((int)Math.round(getCelsius(high2))));
                double high3 = Double.parseDouble(day3High.getText().toString());
                day3High.setText(Integer.toString((int)Math.round(getCelsius(high3))));
                double high4 = Double.parseDouble(day4High.getText().toString());
                day4High.setText(Integer.toString((int)Math.round(getCelsius(high4))));
                double high5 = Double.parseDouble(day5High.getText().toString());
                day5High.setText(Integer.toString((int)Math.round(getCelsius(high5))));
                double high6 = Double.parseDouble(day6High.getText().toString());
                day6High.setText(Integer.toString((int)Math.round(getCelsius(high6))));
                //change lows
                double low1 = Double.parseDouble(day1Low.getText().toString());
                day1Low.setText(Integer.toString((int)Math.round(getCelsius(low1))));
                double low2 = Double.parseDouble(day2Low.getText().toString());
                day2Low.setText(Integer.toString((int)Math.round(getCelsius(low2))));
                double low3 = Double.parseDouble(day3Low.getText().toString());
                day3Low.setText(Integer.toString((int)Math.round(getCelsius(low3))));
                double low4 = Double.parseDouble(day4Low.getText().toString());
                day4Low.setText(Integer.toString((int)Math.round(getCelsius(low4))));
                double low5 = Double.parseDouble(day5Low.getText().toString());
                day5Low.setText(Integer.toString((int)Math.round(getCelsius(low5))));
                double low6 = Double.parseDouble(day6Low.getText().toString());
                day6Low.setText(Integer.toString((int)Math.round(getCelsius(low6))));
            }
        }

        /**
         * modifies the hours on the sunset (assume the sunsets in the evening)
         * if this doesn't hold true, then we have bigger problems than miscalculating
         * when the sun sets
         *
         * @param hours - number of sunset hours
         *
         * @return hours in "PM" format
         *
         */
        public int sunsetModifier(int hours){
            return (hours - 12);
        }

        //updates the UI every minute
        public void minuteUpdater(){
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_TIME_TICK);
            minuteUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    searchZip(inputZipCode);
                }
            };

            registerReceiver(minuteUpdateReceiver, intentFilter);
        }
}

