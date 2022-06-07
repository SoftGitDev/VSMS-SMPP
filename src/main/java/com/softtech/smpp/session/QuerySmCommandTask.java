/*
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.softtech.smpp.session;

import java.io.IOException;
import java.io.OutputStream;

import com.softtech.smpp.PDUSender;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.TypeOfNumber;

/**
 * @author SUTHAR
 *
 */
public class QuerySmCommandTask extends AbstractSendCommandTask {
    private final String messageId;
    private final TypeOfNumber sourceAddrTon;
    private final NumberingPlanIndicator sourceAddrNpi;
    private final String sourceAddr;
    
    public QuerySmCommandTask(PDUSender pduSender,
            String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr) {
        super(pduSender);
        this.messageId = messageId;
        this.sourceAddrTon = sourceAddrTon;
        this.sourceAddrNpi = sourceAddrNpi;
        this.sourceAddr = sourceAddr;
    }
    
    public void executeTask(OutputStream out, int sequenceNumber)
            throws PDUStringException, IOException {
        pduSender.sendQuerySm(out, sequenceNumber, messageId, sourceAddrTon,
                sourceAddrNpi, sourceAddr);
    }
    
    public String getCommandName() {
        return "query_sm";
    }
}
