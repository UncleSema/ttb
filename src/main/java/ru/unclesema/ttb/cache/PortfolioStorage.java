package ru.unclesema.ttb.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortfolioStorage {
    private final Map<String, Integer> lotsByFigi = new HashMap<>();

    public void add(String figi) {
        if (!lotsByFigi.containsKey(figi)) {
            lotsByFigi.put(figi, 1);
        } else {
            lotsByFigi.computeIfPresent(figi, (fig, lots) -> lots + 1);
        }
    }

    public void remove(String figi) {
        if (lotsByFigi.getOrDefault(figi, 0) == 1) {
            lotsByFigi.remove(figi);
        } else {
            lotsByFigi.computeIfPresent(figi, (fig, lots) -> lots - 1);
        }
    }

    public List<String> getAll() {
        List<String> answer = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lotsByFigi.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                answer.add(entry.getKey());
            }
        }
        return answer;
    }

    public boolean contains(String figi) {
        return lotsByFigi.containsKey(figi);
    }
}
