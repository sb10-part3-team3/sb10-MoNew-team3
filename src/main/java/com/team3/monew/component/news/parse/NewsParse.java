package com.team3.monew.component.news.parse;

import com.team3.monew.component.news.record.ParsedData;
import com.team3.monew.component.news.record.RawArticleResult;
import com.team3.monew.entity.enums.NewsSourceType;

public interface NewsParse {

  ParsedData parse(NewsSourceType sourceType, RawArticleResult rawArticle);
}
