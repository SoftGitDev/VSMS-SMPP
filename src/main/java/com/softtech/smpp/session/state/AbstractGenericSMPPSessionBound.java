package com.softtech.smpp.session.state;

import java.io.IOException;

import com.softtech.smpp.GenericNackResponseException;
import com.softtech.smpp.PDUStringException;
import com.softtech.smpp.SMPPConstant;
import com.softtech.smpp.bean.Command;
import com.softtech.smpp.bean.DataSm;
import com.softtech.smpp.bean.DataSmResp;
import com.softtech.smpp.bean.EnquireLinkResp;
import com.softtech.smpp.bean.UnbindResp;
import com.softtech.smpp.extra.PendingResponse;
import com.softtech.smpp.extra.ProcessRequestException;
import com.softtech.smpp.session.BaseResponseHandler;
import com.softtech.smpp.session.DataSmResult;
import com.softtech.smpp.util.DefaultDecomposer;
import com.softtech.smpp.util.IntUtil;
import com.softtech.smpp.util.PDUDecomposer;

/**
 * @author uudashr
 *
 */
abstract class AbstractGenericSMPPSessionBound implements GenericSMPPSessionState {
    protected static final PDUDecomposer pduDecomposer = new DefaultDecomposer();
    
    public void processEnquireLink(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        responseHandler.sendEnquireLinkResp(pduHeader.getSequenceNumber());
    }

    public void processEnquireLinkResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            EnquireLinkResp resp = pduDecomposer.enquireLinkResp(pdu);
            pendingResp.done(resp);
        } else {
        }
    }

    public void processUnbind(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        try {
            responseHandler.sendUnbindResp(pduHeader.getSequenceNumber());
        } finally {
            responseHandler.notifyUnbonded();
        }
    }

    public void processUnbindResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            UnbindResp resp = pduDecomposer.unbindResp(pdu);
            pendingResp.done(resp);
        } else {
        }
    }

    public void processUnknownCid(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        responseHandler.sendGenerickNack(SMPPConstant.STAT_ESME_RINVCMDID,
                pduHeader.getSequenceNumber());
    }
    
    public void processGenericNack(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        if (pendingResp != null) {
            pendingResp.doneWithInvalidResponse(new GenericNackResponseException(
                    "Receive generic_nack with command_status "
                            + pduHeader.getCommandStatusAsHex(), pduHeader.getCommandStatus()));
         }
    }
    
    public void processDataSm(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        try {
            DataSm dataSm = pduDecomposer.dataSm(pdu);
            DataSmResult dataSmResult = responseHandler.processDataSm(dataSm);
            responseHandler.sendDataSmResp(dataSmResult, pduHeader.getSequenceNumber());
        } catch (PDUStringException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        } catch (ProcessRequestException e) {
            responseHandler.sendNegativeResponse(pduHeader.getCommandId(), e.getErrorCode(), pduHeader.getSequenceNumber());
        }
    }
    
    public void processDataSmResp(Command pduHeader, byte[] pdu,
            BaseResponseHandler responseHandler) throws IOException {
        PendingResponse<Command> pendingResp = responseHandler
                .removeSentItem(pduHeader.getSequenceNumber());
        
        if (pendingResp != null) {
            try {
                DataSmResp resp = pduDecomposer.dataSmResp(pdu);
                pendingResp.done(resp);
            } catch (PDUStringException e) {
                responseHandler.sendGenerickNack(e.getErrorCode(), pduHeader
                        .getSequenceNumber());
            }
        } else {
         }
    }
}
