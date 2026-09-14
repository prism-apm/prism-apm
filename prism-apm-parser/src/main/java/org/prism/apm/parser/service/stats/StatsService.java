package org.prism.apm.parser.service.stats;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.LongAdder;

/**
 * @author john
 * @date 2024-10-25 13:50
 */
@Data
@Service
@Slf4j
public class StatsService {


    private LongAdder timeoutCounter = new LongAdder();

    private LongAdder spanSavedCounter = new LongAdder();


    public void countTimeout() {
       timeoutCounter.increment();
   }

    public void countSpanSaved(int size) {
        spanSavedCounter.add(size);
    }


    public String getSpanStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("span stats. ");
        sb.append("saved span: " + spanSavedCounter.longValue() + ", ");
        sb.append("timeout span: " + timeoutCounter.longValue());
        return sb.toString();
    }
}
