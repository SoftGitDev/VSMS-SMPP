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

import com.softtech.smpp.bean.Bind;
import com.softtech.smpp.bean.CancelSm;
import com.softtech.smpp.bean.MessageState;
import com.softtech.smpp.bean.QuerySm;
import com.softtech.smpp.bean.ReplaceSm;
import com.softtech.smpp.bean.SubmitMulti;
import com.softtech.smpp.bean.SubmitMultiResult;
import com.softtech.smpp.bean.SubmitSm;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.util.MessageId;

/**
 * @author SUTHAR
 * 
 */
public interface ServerResponseHandler extends GenericServerResponseHandler {

    void sendSubmitSmResponse(MessageId messageId, int sequenceNumber)
            throws IOException;

    void processBind(Bind bind);

    MessageId processSubmitSm(SubmitSm submitSm) throws ProcessRequestException;
    
    SubmitMultiResult processSubmitMulti(SubmitMulti submitMulti) throws ProcessRequestException;
    
    void sendSubmitMultiResponse(SubmitMultiResult submiitMultiResult,
            int sequenceNumber) throws IOException;
    
    QuerySmResult processQuerySm(QuerySm querySm)
            throws ProcessRequestException;

    void sendQuerySmResp(String messageId, String finalDate,
            MessageState messageState, byte errorCode, int sequenceNumber)
            throws IOException;

    void processCancelSm(CancelSm cancelSm) throws ProcessRequestException;
    
    void sendCancelSmResp(int sequenceNumber) throws IOException;
    
    void processReplaceSm(ReplaceSm replaceSm) throws ProcessRequestException;
    
    void sendReplaceSmResp(int sequenceNumber) throws IOException;
}
