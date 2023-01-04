package zerobase.weather.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;

@SpringBootTest
@Transactional
class JpaMemoRepositoryTest {
    // 테스트 코드 작성
    @Autowired
    JpaMemoRepository jpaMemoRepository;

    @Test
    void insertMemoTest () {
        //given
        Memo newMemo = Memo.builder()
                            .id(10)
                            .text("this is jpa memo")
                            .build();

        //when
        jpaMemoRepository.save(newMemo);

        //then
        List<Memo> memoList = jpaMemoRepository.findAll();
        assertTrue(memoList.size() > 0);

    }


    @DisplayName("Jpa Test")
    @Test
    void findByIdTest() {
        //given
        Memo newMemo = Memo.builder()
                            .id(11)
                            .text("jpa")
                            .build();

        //when
        Memo memo = jpaMemoRepository.save(newMemo);
        System.out.println(memo.getId());

        //then
        Optional<Memo> result = jpaMemoRepository.findById(memo.getId());
        assertEquals(result.get().getText(), "jpa");

    }

}