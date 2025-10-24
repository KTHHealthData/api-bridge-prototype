package com.example.apibridge.Infrastructure;

import com.example.apibridge.Domain.UrlElements;

import java.util.Collection;
import java.util.Map;

public interface IScbApiRepository {
    Collection<Map<String, String>> sendQuery(UrlElements urlElements);
}