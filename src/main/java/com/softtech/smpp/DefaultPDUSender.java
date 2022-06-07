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
import com.softtech.smpp.bean.OptionalParameter.Tag;
import com.softtech.smpp.bean.RegisteredDelivery;
import com.softtech.smpp.bean.ReplaceIfPresentFlag;
import com.softtech.smpp.bean.TypeOfNumber;
import com.softtech.smpp.bean.UnsuccessDelivery;
import com.softtech.smpp.util.DefaultComposer;
import com.softtech.smpp.util.PDUComposer;
import java.text.SimpleDateFormat;

/**
 * The SMPP PDU reader class.
 *
 * @version 1.1
 * @since 1.0
 *
 */
public class DefaultPDUSender implements PDUSender {

    static SimpleDateFormat TimeFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
    private final PDUComposer pduComposer;

    /**
     * Default constructor.
     */
    public DefaultPDUSender() {
        this(new DefaultComposer());
    }

    /**
     * Construct with specified PDU composer.
     *
     * @param pduComposer is the PDU composer.
     */
    public DefaultPDUSender(PDUComposer pduComposer) {
        this.pduComposer = pduComposer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendHeader(java.io.OutputStream, int, int, int)
     */
    public byte[] sendHeader(OutputStream os, int commandId, int commandStatus,
            int sequenceNumber) throws IOException {

        byte[] b = pduComposer.composeHeader(commandId, commandStatus,
                sequenceNumber);
        writeAndFlush(os, b);
        return b;
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

        byte[] b = pduComposer.bind(bindType.commandId(), sequenceNumber,
                systemId, password, systemType, interfaceVersion.value(),
                addrTon.value(), addrNpi.value(), addressRange);
        writeAndFlush(os, b);
        return b;
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

        OptionalParameter[] p;
        if (interfaceVersion != null) {
            OptionalParameter interfaceVersionParam = new OptionalParameter.Byte(Tag.SC_INTERFACE_VERSION, interfaceVersion.value());
            p = new OptionalParameter[]{interfaceVersionParam};
        } else {
            p = new OptionalParameter[]{};
        }
        byte[] b = pduComposer.bindResp(commandId, sequenceNumber, systemId, p);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.softtech.PDUSender#sendOutbind(java.io.OutputStream,
     *      int, java.lang.String, java.lang.String)
     */
    public byte[] sendOutbind(OutputStream os, int sequenceNumber, String systemId, String password)
            throws PDUStringException, IOException {
        byte[] b = this.pduComposer.outbind(sequenceNumber, systemId, password);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendUnbind(java.io.OutputStream, int)
     */
    public byte[] sendUnbind(OutputStream os, int sequenceNumber)
            throws IOException {
        byte[] b = pduComposer.unbind(sequenceNumber);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendGenericNack(java.io.OutputStream, int, int)
     */
    public byte[] sendGenericNack(OutputStream os, int commandStatus,
            int sequenceNumber) throws IOException {
        byte[] b = pduComposer.genericNack(commandStatus, sequenceNumber);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendUnbindResp(java.io.OutputStream, int, int)
     */
    public byte[] sendUnbindResp(OutputStream os, int commandStatus,
            int sequenceNumber) throws IOException {
        byte[] b = pduComposer.unbindResp(commandStatus, sequenceNumber);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendEnquireLink(java.io.OutputStream, int)
     */
    public byte[] sendEnquireLink(OutputStream os, int sequenceNumber)
            throws IOException {

        byte[] b = pduComposer.enquireLink(sequenceNumber);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendEnquireLinkResp(java.io.OutputStream, int)
     */
    public byte[] sendEnquireLinkResp(OutputStream os, int sequenceNumber)
            throws IOException {
        byte[] b = pduComposer.enquireLinkResp(sequenceNumber);
        writeAndFlush(os, b);
        return b;
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
            String destinationAddr, ESMClass esmClass, byte protocoId,
            byte priorityFlag, String scheduleDeliveryTime,
            String validityPeriod, RegisteredDelivery registeredDelivery,
            byte replaceIfPresent, DataCoding dataCoding, byte smDefaultMsgId,
            byte[] shortMessage, OptionalParameter... optionalParameters)
            throws PDUStringException, IOException {
        byte[] b = pduComposer.submitSm(sequenceNumber, serviceType,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                destAddrTon.value(), destAddrNpi.value(), destinationAddr,
                esmClass.value(), protocoId, priorityFlag,
                scheduleDeliveryTime, validityPeriod, registeredDelivery
                        .value(), replaceIfPresent, dataCoding.toByte(),
                smDefaultMsgId, shortMessage, optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendSubmitSmResp(java.io.OutputStream, int,
     *      java.lang.String)
     */
    public byte[] sendSubmitSmResp(OutputStream os, int sequenceNumber,
            String messageId) throws PDUStringException, IOException {
        byte[] b = pduComposer.submitSmResp(sequenceNumber, messageId);
        writeAndFlush(os, b);
        return b;
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
        byte[] b = pduComposer.querySm(sequenceNumber, messageId, sourceAddrTon
                .value(), sourceAddrNpi.value(), sourceAddr);
        writeAndFlush(os, b);
        return b;
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
        byte[] b = pduComposer.querySmResp(sequenceNumber, messageId,
                finalDate, messageState.value(), errorCode);
        writeAndFlush(os, b);
        return b;
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

        byte[] b = pduComposer.deliverSm(sequenceNumber, serviceType,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                destAddrTon.value(), destAddrNpi.value(), destinationAddr,
                esmClass.value(), protocoId, priorityFlag, registeredDelivery
                .value(), dataCoding.toByte(), shortMessage,
                optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.softtech.PDUSender#sendDeliverSmResp(java.io.OutputStream, int, int, String)
     */
    @Override
    public byte[] sendDeliverSmResp(OutputStream os, int commandStatus, int sequenceNumber, String messageId)
            throws IOException {
        byte[] b = pduComposer.deliverSmResp(commandStatus, sequenceNumber, messageId);
        writeAndFlush(os, b);
        return b;
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
        byte[] b = pduComposer.dataSm(sequenceNumber, serviceType,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                destAddrTon.value(), destAddrNpi.value(), destinationAddr,
                esmClass.value(), registeredDelivery.value(), dataCoding
                .toByte(), optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendDataSmResp(OutputStream os, int sequenceNumber,
            String messageId, OptionalParameter... optionalParameters)
            throws PDUStringException, IOException {
        byte[] b = pduComposer.dataSmResp(sequenceNumber, messageId,
                optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendCancelSm(OutputStream os, int sequenceNumber,
            String serviceType, String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            TypeOfNumber destAddrTon, NumberingPlanIndicator destAddrNpi,
            String destinationAddr) throws PDUStringException, IOException {

        byte[] b = pduComposer.cancelSm(sequenceNumber, serviceType, messageId,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                destAddrTon.value(), destAddrNpi.value(), destinationAddr);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendCancelSmResp(OutputStream os, int sequenceNumber)
            throws IOException {

        byte[] b = pduComposer.cancelSmResp(sequenceNumber);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendReplaceSm(OutputStream os, int sequenceNumber,
            String messageId, TypeOfNumber sourceAddrTon,
            NumberingPlanIndicator sourceAddrNpi, String sourceAddr,
            String scheduleDeliveryTime, String validityPeriod,
            RegisteredDelivery registeredDelivery, byte smDefaultMsgId,
            byte[] shortMessage) throws PDUStringException, IOException {
        byte[] b = pduComposer.replaceSm(sequenceNumber, messageId,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                scheduleDeliveryTime, validityPeriod, registeredDelivery
                        .value(), smDefaultMsgId, shortMessage);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendReplaceSmResp(OutputStream os, int sequenceNumber)
            throws IOException {
        byte[] b = pduComposer.replaceSmResp(sequenceNumber);
        writeAndFlush(os, b);
        return b;
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
        byte[] b = pduComposer.submitMulti(sequenceNumber, serviceType,
                sourceAddrTon.value(), sourceAddrNpi.value(), sourceAddr,
                destinationAddresses, esmClass.value(), protocolId,
                priorityFlag, scheduleDeliveryTime, validityPeriod,
                registeredDelivery.value(), replaceIfPresentFlag.value(),
                dataCoding.toByte(), smDefaultMsgId, shortMessage,
                optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendSubmitMultiResp(OutputStream os, int sequenceNumber,
            String messageId, UnsuccessDelivery... unsuccessDeliveries)
            throws PDUStringException, IOException {
        byte[] b = pduComposer.submitMultiResp(sequenceNumber, messageId,
                unsuccessDeliveries);
        writeAndFlush(os, b);
        return b;
    }

    public byte[] sendAlertNotification(OutputStream os, int sequenceNumber,
            byte sourceAddrTon, byte sourceAddrNpi, String sourceAddr,
            byte esmeAddrTon, byte esmeAddrNpi, String esmeAddr,
            OptionalParameter... optionalParameters) throws PDUStringException,
            IOException {
        byte[] b = pduComposer.alertNotification(sequenceNumber, sourceAddrTon,
                sourceAddrNpi, sourceAddr, esmeAddrTon, esmeAddrNpi, esmeAddr,
                optionalParameters);
        writeAndFlush(os, b);
        return b;
    }

    private static void writeAndFlush(OutputStream out, byte[] b)
            throws IOException {
        //System.out.print(TimeFormatter.format(new Date()) + " -->: "+ByteToString(b)+"\n");
        out.write(b);
        out.flush();
    }

    public static String ByteToString(byte[] _bytes) {
       StringBuilder sb = new StringBuilder();
        for(byte b : _bytes){
            sb.append(String.format("%02x", b&0xff));
        }
        return sb.toString();
    }
}
