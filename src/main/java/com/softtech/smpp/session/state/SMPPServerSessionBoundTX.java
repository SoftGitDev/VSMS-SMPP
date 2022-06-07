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
package com.softtech.smpp.session.state;

import java.io.IOException;

import com.softtech.smpp.InvalidNumberOfDestinationsException;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.CancelSm;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.QuerySm;
import com.softtech.smpp.bean.ReplaceSm;
import com.softtech.smpp.bean.SubmitMulti;
import com.softtech.smpp.bean.SubmitMultiResult;
import com.softtech.smpp.bean.SubmitSm;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.QuerySmResult;
import com.softtech.smpp.session.ServerResponseHandler;
import com.softtech.smpp.util.MessageId;

/**
 * @author uudashr
 *
 */
class SMPPServerSessionBoundTX extends SMPPServerSessionBound implements
        SMPPServerSessionState {
     
    public SessionState getSessionState() {
        return SessionState.BOUND_TX;
    }
    
    public void processDeliverSmResp(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        responseHandler.sendNegativeResponse(pduHeader.getCommandId(),
                SMPPConstant.STAT_ESME_RINVBNDSTS, pduHeader
                        .getSequenceNumber());
    }
    
    public void processSubmitSm(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        try {
            SubmitSm submitSm = pduDecomposer.submitSm(pdu);
            MessageId messageId = responseHandler.processSubmitSm(submitSm);
              responseHandler.sendSubmitSmResponse(messageId, pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
    
    public void processSubmitMulti(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        try {
            SubmitMulti submitMulti = pduDecomposer.submitMulti(pdu);
            SubmitMultiResult result = responseHandler.processSubmitMulti(submitMulti);
              responseHandler.sendSubmitMultiResponse(result, pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (InvalidNumberOfDestinationsException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), SMPPConstant.STAT_ESME_RINVNUMDESTS, pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
    
    public void processQuerySm(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        try {
            QuerySm querySm = pduDecomposer.querySm(pdu);
            QuerySmResult result = responseHandler.processQuerySm(querySm);
            responseHandler.sendQuerySmResp(querySm.getMessageId(), 
                    result.getFinalDate(), result.getMessageState(), 
                    result.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
    
    public void processCancelSm(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        try {
            CancelSm cancelSm = pduDecomposer.cancelSm(pdu);
            responseHandler.processCancelSm(cancelSm);
            responseHandler.sendCancelSmResp(pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
    
    public void processReplaceSm(Command pduHeader, byte[] pdu,
            ServerResponseHandler responseHandler) throws IOException {
        try {
            ReplaceSm replaceSm = pduDecomposer.replaceSm(pdu);
            responseHandler.processReplaceSm(replaceSm);
            responseHandler.sendReplaceSmResp(pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
}
