package com.softtech.smpp;

import java.io.IOException;
import java.io.OutputStream;

import com.softtech.smpp.bean.BindType;
import com.softtech.smpp.bean.DataCoding;
import com.softtech.smpp.bean.DestinationAddress;
import com.softtech.smpp.bean.ESMClass;
import com.softtech.smpp.bean.InterfaceVersion;
import com.softtech.smpp.bean.MessageState;
import com.softtech.smpp.bean.NumberingPlanIndicator;
import com.softtech.smpp.bean.OptionalParameter;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.ReplaceIfPresentFlag;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.bean.UnsuccessDelivery;

/**
 * PDU sender with synchronized the {@link OutputStream}.
 * 
 * @author SUTHAR
 * @version 1.1
 * @since 1.0
 * 
 */
public class SynchronizedPDUSender implements PDUSender {
    private final PDUSender pduSender;

    /**
     * Default constructor.
     */
    public SynchronizedPDUSender() {
        this(new DefaultPDUSender());
    }

    /**
     * Construct with specified {@link PDUSender}.
     * 
     * @param pduSender
     */
    public SynchronizedPDUSender(PDUSender pduSender) {
        this.pduSender = pduSender;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendHeader(java.io.OutputStream, int, int, int)
     */
    public byte[] sendHeader(OutputStream os, int commandId, int commandStatus,
            int sequenceNumber) throws IOException {
        synchronized (os) {
            return pduSender.sendHeader(os, commandId, commandStatus,
                    sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendBind(java.io.OutputStream,
     *      com.softtech.BindType, int, java.lang.String, java.lang.String,
     *      java.lang.String, com.softtech.InterfaceVersion,
     *      com.softtech.TypeOfNumber, com.softtech.NumberingPlanIndicator,
     *      java.lang.String)
     */
    public byte[] sendBind(OutputStream os, BindType bindType,
            int sequenceNumber, String systemId, String password,
            String systemType, InterfaceVersion interfaceVersion,
            TypeOfNumber addrTon, NumberingPlanIndicator addrNpi,
            String addressRange) throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendBind(os, bindType, sequenceNumber, systemId,
                    password, systemType, interfaceVersion, addrTon, addrNpi,
                    addressRange);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendBindResp(java.io.OutputStream, int, int,
     *      java.lang.String)
     */
    public byte[] sendBindResp(OutputStream os, int commandId,
            int sequenceNumber, String systemId, InterfaceVersion interfaceVersion) throws PDUStringException,
            IOException {
        synchronized (os) {
            return pduSender.sendBindResp(os, commandId, sequenceNumber,
                    systemId, interfaceVersion);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendUnbind(java.io.OutputStream, int)
     */
    public byte[] sendUnbind(OutputStream os, int sequenceNumber)
            throws IOException {
        synchronized (os) {
            return pduSender.sendUnbind(os, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendGenericNack(java.io.OutputStream, int, int)
     */
    public byte[] sendGenericNack(OutputStream os, int commandStatus,
            int sequenceNumber) throws IOException {
        synchronized (os) {
            return pduSender.sendGenericNack(os, commandStatus, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendUnbindResp(java.io.OutputStream, int, int)
     */
    public byte[] sendUnbindResp(OutputStream os, int commandStatus,
            int sequenceNumber) throws IOException {
        synchronized (os) {
            return pduSender.sendUnbindResp(os, commandStatus, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendEnquireLink(java.io.OutputStream, int)
     */
    public byte[] sendEnquireLink(OutputStream out, int sequenceNumber)
            throws IOException {
        synchronized (out) {
            return pduSender.sendEnquireLink(out, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendEnquireLinkResp(java.io.OutputStream, int)
     */
    public byte[] sendEnquireLinkResp(OutputStream os, int sequenceNumber)
            throws IOException {
        synchronized (os) {
            return pduSender.sendEnquireLinkResp(os, sequenceNumber);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendSubmitSm(java.io.OutputStream, int,
     *      java.lang.String, com.softtech.TypeOfNumber,
     *      com.softtech.NumberingPlanIndicator, java.lang.String,
     *      com.softtech.TypeOfNumber, com.softtech.NumberingPlanIndicator,
     *      java.lang.String, com.softtech.bean.ESMClass, byte, byte,
     *      java.lang.String, java.lang.String,
     *      com.softtech.bean.RegisteredDelivery, byte, com.softtech.bean.DataCoding,
     *      byte, byte[], com.softtech.bean.OptionalParameter[])
     */
    public byte[] sendSubmitSm(OutputStream os, int sequenceNumber,
            String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass, byte protocolId,
            byte priorityFlag, String scheduleDeliveryTime,
            String validityPeriod, RegisteredDelivery registeredDelivery,
            byte replaceIfPresent, DataCoding dataCoding, byte smDefaultMsgId,
            byte[] shortMessage, OptionalParameter... optionalParameters)
            throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendSubmitSm(os, sequenceNumber, serviceType,
                    sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon,
                    destAddrNpi, destinationAddr, esmClass, protocolId,
                    priorityFlag, scheduleDeliveryTime, validityPeriod,
                    registeredDelivery, replaceIfPresent, dataCoding,
                    smDefaultMsgId, shortMessage, optionalParameters);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendSubmitSmResp(java.io.OutputStream, int,
     *      java.lang.String)
     */
    public byte[] sendSubmitSmResp(OutputStream os, int sequenceNumber,
            String messageId) throws PDUStringException, IOException {
        return pduSender.sendSubmitSmResp(os, sequenceNumber, messageId);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendQuerySm(java.io.OutputStream, int,
     *      java.lang.String, com.softtech.TypeOfNumber,
     *      com.softtech.NumberingPlanIndicator, java.lang.String)
     */
    public byte[] sendQuerySm(OutputStream os, int sequenceNumber,
            String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr)
            throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendQuerySm(os, sequenceNumber, messageId,
                    sourceAddrTon, sourceAddrNpi, sourceAddr);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendQuerySmResp(java.io.OutputStream, int,
     *      java.lang.String, java.lang.String, com.softtech.bean.MessageState,
     *      byte)
     */
    public byte[] sendQuerySmResp(OutputStream os, int sequenceNumber,
            String messageId, String finalDate, MessageState messageState,
            byte errorCode) throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendQuerySmResp(os, sequenceNumber, messageId,
                    finalDate, messageState, errorCode);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendDeliverSm(java.io.OutputStream, int,
     *      java.lang.String, com.softtech.TypeOfNumber,
     *      com.softtech.NumberingPlanIndicator, java.lang.String,
     *      com.softtech.TypeOfNumber, com.softtech.NumberingPlanIndicator,
     *      java.lang.String, com.softtech.bean.ESMClass, byte, byte,
     *      com.softtech.bean.RegisteredDelivery, com.softtech.bean.DataCoding,
     *      byte[], com.softtech.bean.OptionalParameter[])
     */
    public byte[] sendDeliverSm(OutputStream os, int sequenceNumber,
            String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass, byte protocoId,
            byte priorityFlag, RegisteredDelivery registeredDelivery,
            DataCoding dataCoding, byte[] shortMessage,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {
        synchronized (os) {
            return pduSender.sendDeliverSm(os, sequenceNumber, serviceType,
                    sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon,
                    destAddrNpi, destinationAddr, esmClass, protocoId,
                    priorityFlag, registeredDelivery, dataCoding, shortMessage,
                    optionalParameters);
        }
    }

    public byte[] sendDeliverSmResp(OutputStream os, int commandStatus, int sequenceNumber, String messageId)
            throws IOException {
        synchronized (os) {
            return pduSender.sendDeliverSmResp(os, commandStatus, sequenceNumber, messageId);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendDataSm(java.io.OutputStream, int,
     *      java.lang.String, com.softtech.TypeOfNumber,
     *      com.softtech.NumberingPlanIndicator, java.lang.String,
     *      com.softtech.TypeOfNumber, com.softtech.NumberingPlanIndicator,
     *      java.lang.String, com.softtech.bean.ESMClass,
     *      com.softtech.bean.RegisteredDelivery, com.softtech.bean.DataCoding,
     *      com.softtech.bean.OptionalParameter[])
     */
    public byte[] sendDataSm(OutputStream os, int sequenceNumber,
            String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr, ESMClass esmClass,
            RegisteredDelivery registeredDelivery, DataCoding dataCoding,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {
        synchronized (os) {
            return pduSender.sendDataSm(os, sequenceNumber, serviceType,
                    sourceAddrTon, sourceAddrNpi, sourceAddr, destAddrTon,
                    destAddrNpi, destinationAddr, esmClass, registeredDelivery,
                    dataCoding, optionalParameters);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendDataSmResp(java.io.OutputStream, int,
     *      java.lang.String, com.softtech.bean.OptionalParameter[])
     */
    public byte[] sendDataSmResp(OutputStream os, int sequenceNumber,
            String messageId, OptionalParameter... optionalParameters)
            throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendDataSmResp(os, sequenceNumber, messageId,
                    optionalParameters);
        }
    }

    public byte[] sendCancelSm(OutputStream os, int sequenceNumber,
            String serviceType, String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr) throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendCancelSm(os, sequenceNumber, serviceType,
                    messageId, sourceAddrTon, sourceAddrNpi, sourceAddr,
                    destAddrTon, destAddrNpi, destinationAddr);
        }
    }

    public byte[] sendCancelSmResp(OutputStream os, int sequenceNumber)
            throws IOException {
        synchronized (os) {
            return pduSender.sendCancelSmResp(os, sequenceNumber);
        }
    }

    public byte[] sendReplaceSm(OutputStream os, int sequenceNumber,
            String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            String scheduleDeliveryTime, String validityPeriod,
            RegisteredDelivery registeredDelivery, byte smDefaultMsgId,
            byte[] shortMessage) throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendReplaceSm(os, sequenceNumber, messageId,
                    sourceAddrTon, sourceAddrNpi, sourceAddr,
                    scheduleDeliveryTime, validityPeriod, registeredDelivery,
                    smDefaultMsgId, shortMessage);
        }
    }

    public byte[] sendReplaceSmResp(OutputStream os, int sequenceNumber)
            throws IOException {
        synchronized (os) {
            return pduSender.sendReplaceSmResp(os, sequenceNumber);
        }
    }

    public byte[] sendSubmiMulti(OutputStream os, int sequenceNumber,
            String serviceType, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            DestinationAddress[] destinationAddresses, ESMClass esmClass,
            byte protocolId, byte priorityFlag, String scheduleDeliveryTime,
            String validityPeriod, RegisteredDelivery registeredDelivery,
            ReplaceIfPresentFlag replaceIfPresentFlag, DataCoding dataCoding,
            byte smDefaultMsgId, byte[] shortMessage,
            OptionalParameter... optionalParameters) throws PDUStringException,
            InvalidNumberOfDestinationsException, IOException {
        synchronized (os) {
            return pduSender.sendSubmiMulti(os, sequenceNumber, serviceType,
                    sourceAddrTon, sourceAddrNpi, sourceAddr,
                    destinationAddresses, esmClass, protocolId, priorityFlag,
                    scheduleDeliveryTime, validityPeriod, registeredDelivery,
                    replaceIfPresentFlag, dataCoding, smDefaultMsgId,
                    shortMessage, optionalParameters);
        }
    }

    public byte[] sendSubmitMultiResp(OutputStream os, int sequenceNumber,
            String messageId, UnsuccessDelivery... unsuccessDeliveries)
            throws PDUStringException, IOException {
        synchronized (os) {
            return pduSender.sendSubmitMultiResp(os, sequenceNumber, messageId,
                    unsuccessDeliveries);
        }
    }

    public byte[] sendAlertNotification(OutputStream os, int sequenceNumber,
            byte sourceAddrTon, byte sourceAddrNpi, String sourceAddr,
            byte esmeAddrTon, byte esmeAddrNpi, String esmeAddr,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {
        synchronized (os) {
            return pduSender.sendAlertNotification(os, sequenceNumber,
                    sourceAddrTon, sourceAddrNpi, sourceAddr, esmeAddrTon,
                    esmeAddrNpi, esmeAddr, optionalParameters);
        }
    }

    public byte[] sendOutbind(OutputStream os, int sequenceNumber, String systemId, String password)
        throws PDUStringException, IOException
    {
        synchronized (os)
        {
            return this.pduSender.sendOutbind(os, sequenceNumber, systemId, password);
        }
    }
}
