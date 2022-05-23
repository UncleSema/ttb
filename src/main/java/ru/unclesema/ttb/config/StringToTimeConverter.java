package ru.unclesema.ttb.config;


import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;

public class StringToTimeConverter implements Converter<String, LocalDateTime> {
    @Override
    public LocalDateTime convert(String from) {
        return LocalDateTime.parse(from);
    }
}
