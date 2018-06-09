/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package alluxio.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class GetMasterInfoTResponse implements org.apache.thrift.TBase<GetMasterInfoTResponse, GetMasterInfoTResponse._Fields>, java.io.Serializable, Cloneable, Comparable<GetMasterInfoTResponse> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("GetMasterInfoTResponse");

  private static final org.apache.thrift.protocol.TField MASTER_INFO_FIELD_DESC = new org.apache.thrift.protocol.TField("masterInfo", org.apache.thrift.protocol.TType.STRUCT, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new GetMasterInfoTResponseStandardSchemeFactory());
    schemes.put(TupleScheme.class, new GetMasterInfoTResponseTupleSchemeFactory());
  }

  private MasterInfo masterInfo; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    MASTER_INFO((short)1, "masterInfo");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // MASTER_INFO
          return MASTER_INFO;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.MASTER_INFO, new org.apache.thrift.meta_data.FieldMetaData("masterInfo", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRUCT        , "MasterInfo")));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(GetMasterInfoTResponse.class, metaDataMap);
  }

  public GetMasterInfoTResponse() {
  }

  public GetMasterInfoTResponse(
    MasterInfo masterInfo)
  {
    this();
    this.masterInfo = masterInfo;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public GetMasterInfoTResponse(GetMasterInfoTResponse other) {
    if (other.isSetMasterInfo()) {
      this.masterInfo = other.masterInfo;
    }
  }

  public GetMasterInfoTResponse deepCopy() {
    return new GetMasterInfoTResponse(this);
  }

  @Override
  public void clear() {
    this.masterInfo = null;
  }

  public MasterInfo getMasterInfo() {
    return this.masterInfo;
  }

  public GetMasterInfoTResponse setMasterInfo(MasterInfo masterInfo) {
    this.masterInfo = masterInfo;
    return this;
  }

  public void unsetMasterInfo() {
    this.masterInfo = null;
  }

  /** Returns true if field masterInfo is set (has been assigned a value) and false otherwise */
  public boolean isSetMasterInfo() {
    return this.masterInfo != null;
  }

  public void setMasterInfoIsSet(boolean value) {
    if (!value) {
      this.masterInfo = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case MASTER_INFO:
      if (value == null) {
        unsetMasterInfo();
      } else {
        setMasterInfo((MasterInfo)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MASTER_INFO:
      return getMasterInfo();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case MASTER_INFO:
      return isSetMasterInfo();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof GetMasterInfoTResponse)
      return this.equals((GetMasterInfoTResponse)that);
    return false;
  }

  public boolean equals(GetMasterInfoTResponse that) {
    if (that == null)
      return false;

    boolean this_present_masterInfo = true && this.isSetMasterInfo();
    boolean that_present_masterInfo = true && that.isSetMasterInfo();
    if (this_present_masterInfo || that_present_masterInfo) {
      if (!(this_present_masterInfo && that_present_masterInfo))
        return false;
      if (!this.masterInfo.equals(that.masterInfo))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_masterInfo = true && (isSetMasterInfo());
    list.add(present_masterInfo);
    if (present_masterInfo)
      list.add(masterInfo);

    return list.hashCode();
  }

  @Override
  public int compareTo(GetMasterInfoTResponse other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetMasterInfo()).compareTo(other.isSetMasterInfo());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMasterInfo()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.masterInfo, other.masterInfo);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("GetMasterInfoTResponse(");
    boolean first = true;

    sb.append("masterInfo:");
    if (this.masterInfo == null) {
      sb.append("null");
    } else {
      sb.append(this.masterInfo);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class GetMasterInfoTResponseStandardSchemeFactory implements SchemeFactory {
    public GetMasterInfoTResponseStandardScheme getScheme() {
      return new GetMasterInfoTResponseStandardScheme();
    }
  }

  private static class GetMasterInfoTResponseStandardScheme extends StandardScheme<GetMasterInfoTResponse> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, GetMasterInfoTResponse struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // MASTER_INFO
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.masterInfo = new MasterInfo();
              struct.masterInfo.read(iprot);
              struct.setMasterInfoIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, GetMasterInfoTResponse struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.masterInfo != null) {
        oprot.writeFieldBegin(MASTER_INFO_FIELD_DESC);
        struct.masterInfo.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class GetMasterInfoTResponseTupleSchemeFactory implements SchemeFactory {
    public GetMasterInfoTResponseTupleScheme getScheme() {
      return new GetMasterInfoTResponseTupleScheme();
    }
  }

  private static class GetMasterInfoTResponseTupleScheme extends TupleScheme<GetMasterInfoTResponse> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, GetMasterInfoTResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetMasterInfo()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetMasterInfo()) {
        struct.masterInfo.write(oprot);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, GetMasterInfoTResponse struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.masterInfo = new MasterInfo();
        struct.masterInfo.read(iprot);
        struct.setMasterInfoIsSet(true);
      }
    }
  }

}

