package zerobase.weather.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.WeatherApplication;
import zerobase.weather.domain.DateWeather;
import zerobase.weather.domain.Diary;
import zerobase.weather.error.InvalidDate;
import zerobase.weather.repository.DateWeatherRepository;
import zerobase.weather.repository.DiaryRepository;

@Service
@Transactional(readOnly = true)
public class DiaryService {

    @Value("${openweathermap.key}")
    private String apikey;

    // DB 연동
    private final DiaryRepository diaryRepository;

    private final DateWeatherRepository dateWeatherRepository;

    private static final Logger logger = LoggerFactory.getLogger(WeatherApplication.class);

    public DiaryService(DiaryRepository diaryRepository,
        DateWeatherRepository dateWeatherRepository) {
        this.diaryRepository = diaryRepository;
        this.dateWeatherRepository = dateWeatherRepository;
    }

    // 매일 1시에 날씨 데이터 저장해줌
    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    public void saveWeatherDate() {
        logger.info("오늘도 날씨 데이터 잘 가져옴");
        dateWeatherRepository.save(getWeatherFromApi());
    }

    private DateWeather getWeatherFromApi() {
        // open weather map에서 날씨 데이터 가져오기
        String weatherData = getWeatherString();

        // 받아온 날씨 데이터 json 파싱하기
        Map<String, Object> parsedWeather = parseWeather(weatherData);
        DateWeather dateWeather = new DateWeather();
        dateWeather.setDate(LocalDate.now());
        dateWeather.setWeather(parsedWeather.get("main").toString());
        dateWeather.setIcon(parsedWeather.get("icon").toString());
        dateWeather.setTemperature((Double) parsedWeather.get("temp"));
        return dateWeather;

    }

    private DateWeather getDateWeather(LocalDate date) {
        List<DateWeather> dateWeatherListFromDB = dateWeatherRepository.findAllByDate(date);
        if (dateWeatherListFromDB.size() == 0) {
            // 새로 api에서 날씨 정보를 가져와야 한다
            // 정책상... 현재 날씨를 가져오거나, 날씨없이 일기를 쓰도록...
            return getWeatherFromApi();
        } else {
            return dateWeatherListFromDB.get(0);
        }
    }

    // 날씨 일기 생성
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void createDiary(LocalDate date, String text) {
        logger.info("started to create diary");

        // 날씨 데이터 가져오기 (API op DB)
        DateWeather dateWeather = getDateWeather(date);

        // 파싱된 데이터를 일기 값 db에 넣기
        Diary nowDiary = new Diary();
        nowDiary.setDateWeather(dateWeather);
        nowDiary.setText(text);

        // DB에 저장
        diaryRepository.save(nowDiary);

    }


    // 날짜별 날씨일기 조회
    @Transactional(readOnly = true)
    public List<Diary> readDiary(LocalDate date) {

//        if (date.isAfter(LocalDate.ofYearDay(3050, 1))) {
//            throw new InvalidDate();
//        }

        return diaryRepository.findAllByDate(date);
    }

    // 기간별 날씨일기 조회
    public List<Diary> readDiaries(LocalDate startDate, LocalDate endDate) {
        return diaryRepository.findAllByDateBetween(startDate, endDate);
    }

    // 일기 수정
    public void updateDiary(LocalDate date, String text) {
        Diary nowDiary = diaryRepository.getFirstByDate(date);
        nowDiary.setText(text);
        diaryRepository.save(nowDiary);
    }

    // 날씨일기 삭제
    public void deleteDiary(LocalDate date) {
        diaryRepository.deleteAllByDate(date);
    }

    // OpenWeatherMap에서 데이터 받아오기
    private String getWeatherString() {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=seoul&appid=" + apikey;

        try {
            URL url = new URL(apiUrl);

            // apiUrl을 Http형식으로 연결
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // 응답코드
            int responseCode = connection.getResponseCode();
            BufferedReader br;

            if (responseCode == 200) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream())); // 정상
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream())); // Error
            }

            String inputLine;
            StringBuilder response = new StringBuilder();

            // 버퍼에 넣어두었던 데이터를 하나하나 읽어옴
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);                 // StringBuilder 객체에 결과값을 쌓음
            }

            br.close();
            return response.toString();

        } catch (Exception e) {
            return "failed to get response";
        }
    }

    // 날씨 데이터를 String으로 받아서 JSONParser를 이용해서 파싱
    private Map<String, Object> parseWeather(String jsonString) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;

        try {
            jsonObject = (JSONObject) jsonParser.parse(jsonString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> resultMap = new HashMap<>();

        // 받아온 데이터 (json) 사용 가능하게 파싱
        // "main":{"temp":275.04,"feels_like":272.83,
        // "temp_min":274.84,"temp_max":275.81,"pressure":1031,"humidity":35}
        // "weather":[{"id":800,"main":"Clear","description":"clear sky","icon":"01d"}

        JSONObject mainData = (JSONObject) jsonObject.get("main");          // weather.main(정보)
        resultMap.put("temp", mainData.get("temp"));                        // main
        JSONArray weatherArray = (JSONArray) jsonObject.get("weather");
        JSONObject weatherData = (JSONObject) weatherArray.get(0);          // 리스트 안에 객체가 1개
        resultMap.put("main", weatherData.get("main"));
        resultMap.put("icon", weatherData.get("icon"));                     // icon
        return resultMap;
    }

}
