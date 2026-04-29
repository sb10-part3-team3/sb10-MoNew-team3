package com.team3.monew.component.news.parse;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;
import com.team3.monew.exception.news.NewsIllegalBeanException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsParser {

  private final Map<String, NewsParse> newsParse;

  public ParsedData parse(NewsSourceType sourceType, RawArticleResult rawArticleResult) {
    String beanName = sourceType.name().toLowerCase() + "NewsParse";
    NewsParse parser = newsParse.get(beanName);
    if (parser == null) {
      throw new NewsIllegalBeanException(Map.of("NewsParse", beanName));
    }

    log.debug("NewsParser: {} 시작", beanName);
    return parser.parse(sourceType, rawArticleResult);
  }
}
