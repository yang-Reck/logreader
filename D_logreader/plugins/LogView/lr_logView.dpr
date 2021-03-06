library lr_logView;

{ Important note about DLL memory management: ShareMem must be the
  first unit in your library's USES clause AND your project's (select
  Project-View Source) USES clause if your DLL exports any procedures or
  functions that pass strings as parameters or function results. This
  applies to all strings passed to and from your DLL--even those that
  are nested in records and classes. ShareMem is the interface unit to
  the BORLNDMM.DLL shared memory manager, which must be deployed along
  with your DLL. To avoid using BORLNDMM.DLL, pass string information
  using PChar or ShortString parameters. }

uses
  SysUtils,
  Classes,
  logdisplay in 'logdisplay.pas' {frm_logdisplay},
  contextCode in '..\..\contextCode.pas',
  p_structDefine in '..\..\p_structDefine.pas',
  OpCode in '..\..\OpCode.pas',
  Memory_Common in 'H:\Delphi\通用的自定义单元\Memory_Common.pas',
  Winapi.Windows;

const
  STATUS_SUCCESS = $00000000;   //成功

const
  CurrentPluginVersion = 100;


/// <summary>
/// 插件信息
/// </summary>
/// <param name="shortname">输出插件名称</param>
/// <returns>当前插件版本</returns>
function _Lr_PluginInfo(shortname: PChar): integer; stdcall;
begin
  StrCopy(shortname, 'lr_logView');
  Result := CurrentPluginVersion;
end;

/// <summary>
/// 初始化插件调用
/// </summary>
/// <param name="engineVersion">调用系统版本</param>
/// <returns>状态标识</returns>
function _Lr_PluginInit(engineVersion: Integer): integer; stdcall;
begin
  OutputDebugString('xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
//  frm_logdisplay := Tfrm_logdisplay.Create(nil);
//  frm_logdisplay.Show;
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// 释放插件
/// </summary>
/// <returns>状态标识</returns>
function _Lr_PluginUnInit(): integer; stdcall;
begin
//  frm_logdisplay.Free;
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// 获取插件中单独定义的错误
/// </summary>
/// <param name="StatusCode">状态标识</param>
/// <param name="eMsg">状态标识的描述信息</param>
/// <returns></returns>
function _Lr_PluginGetErrMsg(StatusCode: Cardinal; eMsg:PChar ): integer; stdcall;
begin
  if StatusCode = STATUS_SUCCESS then
  begin
    StrCopy(eMsg, '成功');
  end
  else
  begin
    StrCopy(eMsg, '未定义的错误！！');
  end;
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// 注册读取行信息时的回调
/// </summary>
/// <param name="lsn"></param>
/// <param name="Raw"></param>
/// <returns>状态标识</returns>
function _Lr_PluginRegLogRowRead(lsn: Plog_LSN; Raw: PMemory_data): integer; stdcall;
begin
  //NotifySubscribe(lsn^, Raw^);
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// 注册 事务包回调
/// </summary>
/// <param name="TransPkg"></param>
/// <returns>状态标识</returns>
function _Lr_PluginRegTransPkg(TransPkg: PMemory_data): integer; stdcall;
var
  tranId: PTrans_Id;
  RecCount: Integer;
begin
  //////////////////////////////////////////////////////////////////////////
  ///                             bin define
  /// |tranID|rowCount|每行长度的数组|行数据
  ///   4        2       4*rowCount       x
  ///
  //////////////////////////////////////////////////////////////////////////
  tranId := TransPkg.data;
  RecCount := PWord(UIntPtr(TransPkg.data) + SizeOf(TTrans_Id))^;
  outputdebugString(PChar(Format('tranId:%s, len:%d', [TranId2Str(tranId^), RecCount])));
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// Sql语句
/// </summary>
/// <param name="Sql"></param>
/// <returns></returns>
function _Lr_PluginRegSQL(Sql: PChar): integer; stdcall;
begin
  Result := STATUS_SUCCESS;
end;

/// <summary>
/// XML打包记录
/// </summary>
/// <param name="Xml"></param>
/// <returns></returns>
function _Lr_PluginRegXML(Xml: PChar): integer; stdcall;
begin
  Result := STATUS_SUCCESS;
end;

exports
  _Lr_PluginInfo,
  _Lr_PluginInit,
  _Lr_PluginUnInit,
  _Lr_PluginGetErrMsg,
  _Lr_PluginRegTransPkg,
  _Lr_PluginRegLogRowRead,
  _Lr_PluginRegSQL,
  _Lr_PluginRegXML;

begin


end.

