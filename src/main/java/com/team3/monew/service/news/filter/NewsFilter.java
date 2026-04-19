package com.team3.monew.service.news.filter;

import com.team3.monew.service.NewsParseService.ParsedData;
import com.team3.monew.service.NewsParseService.ParsedNewsArticle;
import java.util.List;
import java.util.Set;

public interface NewsFilter {

  List<ParsedNewsArticle> filterKeyword(List<ParsedData> parsedData, Set<String> keywords);
}
