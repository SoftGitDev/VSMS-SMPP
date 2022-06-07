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

import com.softtech.smpp.InvalidResponseException;
import com.softtech.smpp.PDUException;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.extra.NegativeResponseException;
import com.softtech.smpp.extra.ResponseTimeoutException;

/**
 * @author SUTHAR
 *
 */
public interface SMPPOperation {
    
    void unbind() throws ResponseTimeoutException, InvalidResponseException, IOException;
    
    void unbindResp(int sequenceNumber) throws IOException;
    
    DataSmResult dataSm(String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass,
            RegisteredDelivery registeredDelivery, DataCoding dataCoding,
            OptionalParameter... optionalParameters) throws PDUException,
            ResponseTimeoutException, InvalidResponseException,
            NegativeResponseException, IOException;
    
    void dataSmResp(int sequenceNumber, String messageId,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException;
    
    void enquireLink() throws ResponseTimeoutException,
            InvalidResponseException, IOException;
        
    void enquireLinkResp(int sequenceNumber) throws IOException;
    
    void genericNack(int commandStatus, int sequenceNumber) throws IOException;
}
