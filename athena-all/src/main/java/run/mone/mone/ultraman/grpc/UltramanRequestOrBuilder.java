// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: service.proto

package run.mone.mone.ultraman.grpc;

public interface UltramanRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:com.xiaomi.mone.ultraman.grpc.UltramanRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string id = 1;</code>
   */
  String getId();
  /**
   * <code>string id = 1;</code>
   */
  com.google.protobuf.ByteString
      getIdBytes();

  /**
   * <code>string cmd = 2;</code>
   */
  String getCmd();
  /**
   * <code>string cmd = 2;</code>
   */
  com.google.protobuf.ByteString
      getCmdBytes();

  /**
   * <code>string params = 3;</code>
   */
  String getParams();
  /**
   * <code>string params = 3;</code>
   */
  com.google.protobuf.ByteString
      getParamsBytes();

  /**
   * <code>map&lt;string, string&gt; paramMap = 4;</code>
   */
  int getParamMapCount();
  /**
   * <code>map&lt;string, string&gt; paramMap = 4;</code>
   */
  boolean containsParamMap(
      String key);
  /**
   * Use {@link #getParamMapMap()} instead.
   */
  @Deprecated
  java.util.Map<String, String>
  getParamMap();
  /**
   * <code>map&lt;string, string&gt; paramMap = 4;</code>
   */
  java.util.Map<String, String>
  getParamMapMap();
  /**
   * <code>map&lt;string, string&gt; paramMap = 4;</code>
   */

  String getParamMapOrDefault(
      String key,
      String defaultValue);
  /**
   * <code>map&lt;string, string&gt; paramMap = 4;</code>
   */

  String getParamMapOrThrow(
      String key);
}