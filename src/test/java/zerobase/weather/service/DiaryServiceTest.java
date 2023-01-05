package zerobase.weather.service;

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Diary;
import zerobase.weather.repository.DiaryRepository;

@SpringBootTest
@Transactional
class DiaryServiceTest {

    @Autowired
    private DiaryRepository diaryRepository;

    @Autowired
    private DiaryService diaryService;

    @DisplayName("날씨 일기 작성")
    @Test
    void createDiary() {
        // given
        Diary newDiary = Diary.builder()
            .id(1)
            .weather("Clouds")
            .icon("04n")
            .temperature(263.81)
            .text("오늘 코딩 너무 재밌었다")
            .date(LocalDate.now())
            .build();

        // when
        diaryRepository.save(newDiary);

        // then
        Optional<Diary> result = Optional.ofNullable(diaryRepository.findAll().get(0));
        assertEquals(4, diaryRepository.count());

        assertEquals(result.get().getId(), 1);
        assertEquals(result.get().getWeather(), "Clouds");
        assertEquals(result.get().getIcon(), "04n");
        assertEquals(result.get().getTemperature(), 263.81);
        assertEquals(result.get().getText(), "오늘 코딩 너무 재밌었다");
        assertEquals(result.get().getDate(), LocalDate.now());

    }

    @Test
    void deleteDiary() {
        // given
        Diary newDiary = Diary.builder()
            .id(1)
            .weather("Clouds")
            .icon("04n")
            .temperature(263.81)
            .text("오늘 코딩 너무 재밌었다")
            .date(LocalDate.parse("2022-12-22"))
            .build();

        diaryRepository.save(newDiary);

        // when
        diaryService.deleteDiary(newDiary.getDate());

        // then
        Assertions.assertEquals(3, diaryRepository.count());
    }
}