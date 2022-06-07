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

import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.CancelSmResp;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.QuerySmResp;
import com.softtech.smpp.bean.ReplaceSmResp;
import com.softtech.smpp.bean.SubmitMultiResp;
import com.softtech.smpp.bean.SubmitSmResp;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.SessionState;
import com.softtech.smpp.session.ResponseHandler;

/**
 * This class is bound_tx state implementation of {@link SMPPSessionState}. This
 * class give specific response to a transmit related transaction, otherwise it
 * always give negative response.
 *
 * @author uudashr
 * @version 1.0
 * @since 2.0
 *
 */
class SMPPSessionBoundTX extends SMPPSessionBound implements SMPPSessionState {

    private static final String NO_REQUEST_FIND_FOR_SEQUENCE_NUMBER = "No request find for sequence number ";

    public SessionState getSessionState() {
        return SessionState.BOUND_TX;
    }

    public void processSubmitSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {

        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            try {
                SubmitSmResp resp = pduDecomposer.submitSmResp(pdu);
                pendingResp.done(resp);
            } catch (PDUStringException e) {
                responseHandler.sendGenerickNack(e.getErrorCode(), pduHeader
                        .getSequenceNumber());
            }
        } else {
        }
    }

    public void processSubmitMultiResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            try {
                SubmitMultiResp resp = pduDecomposer.submitMultiResp(pdu);
                pendingResp.done(resp);
            } catch (PDUStringException e) {
                responseHandler.sendGenerickNack(e.getErrorCode(), pduHeader
                        .getSequenceNumber());
            }
        } else {
        }
    }

    public void processQuerySmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {

        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            try {
                QuerySmResp resp = pduDecomposer.querySmResp(pdu);
                pendingResp.done(resp);
            } catch (PDUStringException e) {
                responseHandler.sendGenerickNack(e.getErrorCode(), pduHeader
                        .getSequenceNumber());
            }
        } else {
        }
    }

    public void processCancelSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            CancelSmResp resp = pduDecomposer.cancelSmResp(pdu);
            pendingResp.done(resp);
        } else {
        }
    }

    public void processReplaceSmResp(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            ReplaceSmResp resp = pduDecomposer.replaceSmResp(pdu);
            pendingResp.done(resp);
        } else {
        }
    }

    public void processDeliverSm(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) throws IOException {
        responseHandler.sendNegativeResponse(pduHeader.getCommandId(),
                SMPPConstant.STAT_ESME_RINVBNDSTS, pduHeader
                        .getSequenceNumber());
    }

    public void processAlertNotification(Command pduHeader, byte[] pdu,
            ResponseHandler responseHandler) {
    }
}
