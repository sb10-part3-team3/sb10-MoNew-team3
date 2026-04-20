package com.team3.monew.global.init;

import com.team3.monew.entity.NewsSource;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.repository.NewsSourceRepository;
import com.team3.monew.service.NewsCollectService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NewsSourceInitializer implements CommandLineRunner {

  private final NewsSourceRepository newsSourceRepository;
  // test용
  private final NewsCollectService syncService;

  @Override
  public void run(String... args) throws Exception {
    // Source 초기화
    NewsSource sourceNaver = NewsSource.create("NAVER", NewsSourceType.NAVER,
        "https://openapi.naver.com");
    NewsSource sourceChosun = NewsSource.create("CHOSUN", NewsSourceType.CHOSUN,
        "https://www.chosun.com");
    List<NewsSource> sources = List.of(sourceNaver, sourceChosun);

    // DB에 이미 있는 소스들의 이름 추출
    Set<String> existingNames = newsSourceRepository.findAll().stream()
        .map(NewsSource::getName)
        .collect(Collectors.toSet());

    // 생성하려는 Source 중에 없는 항목들만 필터링
    List<NewsSource> newSources = sources.stream()
        .filter(src -> !existingNames.contains(src.getName()))
        .toList();

    // 없는 새로운 Source 저장
    if (!newSources.isEmpty()) {
      newsSourceRepository.saveAll(newSources);
    }
  }
}
