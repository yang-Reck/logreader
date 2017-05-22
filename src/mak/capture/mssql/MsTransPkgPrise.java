package mak.capture.mssql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import mak.capture.DBLogPriser;
import mak.capture.data.DBOptInsert;
import mak.capture.data.DBOptUpdate;
import mak.data.input.GenericLittleEndianAccessor;
import mak.data.input.SeekOrigin;
import mak.tools.ArrayUtil;
import mak.tools.HexTool;

public class MsTransPkgPrise {
	private static Logger logger = Logger.getLogger(MsTransPkgPrise.class);  
	private MsTransPkg mPkg;
	private MsDict md;
	
	private ArrayList<DBLogPriser> Values= new ArrayList<>();
	private HashMap<String, byte[]> LCX_TEXT_MIX = new HashMap<>();
	public String aJobStr;
	
	
	public MsTransPkgPrise(MsTransPkg mPkg, MsDict md) {
		this.mPkg = mPkg;
		this.md = md;
	}
	
	/**
	 * 蝕兵蛍盾晩崗屈序崙
	 */
	public void start(){
		if (mPkg.actions.size() <= 2) {
			return;
		}
		
		for (MsLogRowData mlrd : mPkg.actions) {
			try{
				if ("LOP_INSERT_ROWS".equals(mlrd.operation)) {
					if ("LCX_TEXT_MIX".equals(mlrd.context)) {
						String Key = mlrd.pageFID +":" + mlrd.pagePID +":" +mlrd.slotid; 
						LCX_TEXT_MIX.put(Key, mlrd.r0);
					}else if ("LCX_HEAP".equals(mlrd.context) || "LCX_CLUSTERED".equals(mlrd.context)) {
						mlrd.table = md.list_MsTable.get(mlrd.obj_id);
						if (mlrd.table == null) {
							logger.error("盾裂晩崗払移��LOP_INSERT_ROWS.obj_id涙丼 LSN��" + mlrd.LSN);
							return;
						}
						DBOptInsert dbi = PriseInsertLog_LOP_INSERT_ROWS(mlrd);
						System.out.println(dbi.BuildSql());
					}else{
						logger.error("盾裂晩崗払移��LOP_INSERT_ROWS隆岑議context��"+mlrd.context+" LSN��" + mlrd.LSN);
						return;
					}
					
				}else if ("LOP_MODIFY_ROW".equals(mlrd.operation)) {
					//LOP_MODIFY_ROW議芝村 Offset 恬厚仟児彈
					if ("LCX_TEXT_MIX".equals(mlrd.context)) {
						String Key = mlrd.pageFID +":" + mlrd.pagePID +":" +mlrd.slotid; 
						byte[] olddata = LCX_TEXT_MIX.get(Key);
						byte[] newdata = new byte[mlrd.offset + mlrd.r1.length];
						System.arraycopy(olddata, 0, newdata, 0, mlrd.offset);
						System.arraycopy(mlrd.r1, 0, newdata, mlrd.offset, mlrd.r1.length);	
						LCX_TEXT_MIX.replace(Key, newdata);
					}else if ("LCX_HEAP".equals(mlrd.context) || "LCX_CLUSTERED".equals(mlrd.context)) {
						mlrd.table = md.list_MsTable.get(mlrd.obj_id);
						if (mlrd.table == null) {
							logger.error("盾裂晩崗払移��LOP_INSERT_ROWS.obj_id涙丼 LSN��" + mlrd.LSN);
							return;
						}
						DBOptUpdate dbu = PriseUpdateLog_LOP_MODIFY_ROW(mlrd);
						System.out.println(dbu.BuildSql());
						
					}else{
						logger.error("盾裂晩崗払移��LOP_MODIFY_ROW隆岑議context��"+mlrd.context+" LSN��" + mlrd.LSN);
						return;
					}
				}else if ("LOP_MODIFY_COLUMNS".equals(mlrd.operation)) {
					if ("LCX_CLUSTERED".equals(mlrd.context)) {
						mlrd.table = md.list_MsTable.get(mlrd.obj_id);
						if (mlrd.table == null) {
							logger.error("盾裂晩崗払移��LOP_INSERT_ROWS.obj_id涙丼 LSN��" + mlrd.LSN);
							return;
						}
						DBOptUpdate dbu = PriseUpdateLog_LOP_MODIFY_COLUMNS2(mlrd);
						System.out.println(dbu.BuildSql());
					}else{
						logger.error("盾裂晩崗払移��LOP_MODIFY_COLUMNS隆岑議context��"+mlrd.context+" LSN��" + mlrd.LSN);
						return;
					}
				}else if ("LOP_DELETE_ROWS".equals(mlrd.operation)) {
					if ("LCX_TEXT_MIX".equals(mlrd.context)) {
						//屈序崙方象��評茅
					}else if ("LCX_HEAP".equals(mlrd.context) || "LCX_MARK_AS_GHOST".equals(mlrd.context)){
						//LOP_DELETE_ROWS	LCX_MARK_AS_GHOST
						mlrd.table = md.list_MsTable.get(mlrd.obj_id);
						if (mlrd.table == null) {
							logger.error("盾裂晩崗払移��LOP_INSERT_ROWS.obj_id涙丼 LSN��" + mlrd.LSN);
							return;
						}
						DBOptInsert dbi = PriseInsertLog_LOP_INSERT_ROWS(mlrd);
						System.out.println(BuildDeleteSql(dbi, mlrd));
						
					}else{
						logger.error("盾裂晩崗払移��LOP_DELETE_ROWS隆岑議context��"+mlrd.context+" LSN��" + mlrd.LSN);
						return;
					}
				}
			}catch(Exception eee){
				logger.error("盾裂晩崗払移�［湮� LSN��" + mlrd.LSN, eee);
			}
		}
	}
	private String BuildDeleteSql(DBOptInsert dbi, MsLogRowData mlrd){
		if (dbi == null) {
			return "";
		}
		String s2 = "";
		if (mlrd.table.PrimaryKey != null && mlrd.table.PrimaryKey.Fields != null && mlrd.table.PrimaryKey.Fields.size() > 0) {
			for (MsColumn msColumn : mlrd.table.PrimaryKey.Fields) {
				int idx = dbi.Fields.indexOf(msColumn);
				s2 += " and " + MsFunc.BuildSegment(msColumn, dbi.Values.get(idx));
			}
		}
		else{
			//短嗤麼囚祥功象侭嗤忖粁伏撹where
			for (int i = 0; i < dbi.Fields.size(); i++) {
				if (canBeWhereSegColType(dbi.Fields.get(i))) {
					s2 += " and " + MsFunc.BuildSegment(dbi.Fields.get(i), dbi.Values.get(i));
				}
			}
		}
		s2 = s2.substring(5);
		return String.format("DELETE %s where %s", mlrd.table.GetFullName(), s2);
	}
	
	private void align4Byte(GenericLittleEndianAccessor glea){
		int position = glea.getBytesRead();
		glea.seek((position+3)&0xFFFFFFFC, SeekOrigin.soFromBeginning);
	}

	private DBOptUpdate PriseUpdateLog_LOP_MODIFY_COLUMNS2(MsLogRowData mlrd) {
		DBOptUpdate res = new DBOptUpdate();
		res.tableName = mlrd.table.GetFullName();
		res.obj_id = mlrd.table.id;
		GenericLittleEndianAccessor glea = new GenericLittleEndianAccessor(mlrd.LogRecord);
		glea.skip(2);
		int NumElementsOffset = glea.readShort();
		glea.seek(NumElementsOffset, SeekOrigin.soFromBeginning);
		
		int NumElements = glea.readShort();
		short[] elements = new short[NumElements]; 
		for (int i = 0; i < NumElements; i++) {
			elements[i] = glea.readShort();
		}
		if(elements[2] == 0){
			//TODO:!!!!!!!!短嗤麼囚��楹楹楹
			logger.warn("方象垂��"+md.Db.GetFullDbName()+"��燕:"+ mlrd.table.GetFullName()+"涙麼囚�仝�仟荷恬函���。。�");
			return null;
		}
		//蝕兵議沫哈海業匯違 = 2+厚仟曝囃*2。及匯、屈了蛍艶頁析方象才仟方象議nullmap蝕兵了崔
		byte[] offsetOfUpdatedCell = glea.read(elements[0]);
		byte[] UNKNOWN = glea.read(elements[1]);
		align4Byte(glea);
		mlrd.r2 = glea.read(elements[2]);  //佩麼囚
		align4Byte(glea);
		byte[] TableInfo = glea.read(elements[3]);//厚仟燕議麼勣佚連��泌燕議Object_id
		align4Byte(glea);
		
		//厚仟曝囃方。�狒攀鍔峩虜浪�欺匯倖曝囃
		int UpdateRangeCount = elements[0] / 4;
		int nullMapLength = (mlrd.table.getNullMapSorted_Columns().length + 7) >>> 3;
		int varDataIdxOffset = mlrd.table.theFixedLength + nullMapLength + 2;
		
		boolean includeVarCol = false;
		ArrayList<MsColumn> UnLocalValCol = new ArrayList<>();
		short[] idxs = null;
		//lucky�。。。�辛參功象晩崗盾裂��厚仟念朔議峙
		for (int i = 0; i < UpdateRangeCount; i++) {
			byte[] oldValue = glea.read(elements[4 + i * 2]);
			align4Byte(glea);
			byte[] newValue = glea.read(elements[4 + i * 2 + 1]);
			align4Byte(glea);
			
			GenericLittleEndianAccessor value_glea = new GenericLittleEndianAccessor(newValue);
//			int OldValueStartOffset = ArrayUtil.getBytesShort(offsetOfUpdatedCell, i * 2);

			int NewValueStartOffset = ArrayUtil.getBytesShort(offsetOfUpdatedCell, i * 4 + 2);				
			if (NewValueStartOffset != 0) {
				if (NewValueStartOffset > varDataIdxOffset) {
					//延海双��遇拝嗽短嗤朕村燕��方象駅倬勣響方象垂函峙阻��
					if (idxs == null || idxs.length == 0) {
						includeVarCol = true;
					}else{
						//朕村燕嗤丼��傍苧辛嬬珊嗤照
						MsColumn msColumn = null;
						//柴麻蝕兵厚仟議双��阿欺厚仟議曾倖双頁慧欺匯翠戦中議
						int ColIdx = -1;
						int datalen = -1;
						for (int j = 0; j < idxs.length; j++) {
							if(idxs[j] == NewValueStartOffset){
								ColIdx = j + 1;
								datalen = idxs[j + 1] - idxs[j];
								break;
							}
						}
						if (ColIdx < 0) {
							//貫双方象嶄寂了崔蝕兵亟議��耐。。。
							includeVarCol = true;
						}else{
							while(true){
								if (datalen > 0) {
									msColumn = mlrd.table.getSorted_VariantColumns()[ColIdx];
									byte[] sval = value_glea.read(datalen);
									String TmpStr = MsFunc.BuildSegment(msColumn, sval);
									res.NewValues.add(TmpStr);
								}
								if (value_glea.available() > 0) {
									datalen = idxs[ColIdx + 1] - idxs[ColIdx];
									ColIdx += 1;
								}else{
									break;
								}
							}
						}
					}
				}else if (NewValueStartOffset < mlrd.table.theFixedLength) {
					//協海双��
					int writeOffset = NewValueStartOffset;
					for (MsColumn mColumn : mlrd.table.getNullMapSorted_Columns()) {
						if (mColumn.leaf_pos > 0) {
							if (writeOffset == mColumn.theRealPosition) {
								//孀欺双
//								String TmpStr = MsFunc.BuildSegment(mColumn, mlrd.r0);
//								res.OldValues.add(TmpStr);
								
								if (newValue.length < mColumn.max_length) {
									//峪芝村阻方象念磯何蛍
									
									UnLocalValCol.add(mColumn);
									break;
								}else{
									byte[] sval = value_glea.read(mColumn.max_length);
									String TmpStr = MsFunc.BuildSegment(mColumn, sval);
									res.NewValues.add(TmpStr);
									
									if (value_glea.available() > 0) {
										//傍苧双朔中珊嗤凪万双方象
										writeOffset = mColumn.theRealPosition + mColumn.max_length;
									}else{
										break;
									}
								}
							}else if (writeOffset > mColumn.theRealPosition && writeOffset < mColumn.theRealPosition + mColumn.max_length){
							    //壓双峙袈律坪�。�俶勣恵諒方象垂臥孀寔糞方象
								UnLocalValCol.add(mColumn);
								int dlen = Math.min(mColumn.theRealPosition + mColumn.max_length - writeOffset, value_glea.available());
								value_glea.skip(dlen);
								if (value_glea.available() > 0) {
									//傍苧双朔中珊嗤凪万双方象
									writeOffset = writeOffset - mColumn.theRealPosition + mColumn.max_length;
								}else{
									break;
								}
							}
						}
					}
				}else{
					//顕固阻NullMap��心心嗤短嗤頼屁議延海双朕村��嗤議三祥嬬岷俊函峙��
					int OverlapNullMapLen = nullMapLength - (NewValueStartOffset - mlrd.table.theFixedLength);
					byte[] nullMap = value_glea.read(OverlapNullMapLen);
					int idxsCount = value_glea.readShort();
					idxs = new short[idxsCount];
					for (int j = 0; j < idxsCount; j++) {
						if (value_glea.available() < 2) {
							break;
						}
						idxs[j] = value_glea.readShort();
					}
					if (value_glea.available()>0) {
						//響頼朕村燕��泌惚珊嗤峙��祥頁方象双阻
						NewValueStartOffset +=  OverlapNullMapLen + 2 + idxs.length * 2; 
						
						//柴麻蝕兵厚仟議双��阿欺厚仟議曾倖双頁慧欺匯翠戦中議
						int ColIdx = 0;
						int datalen = idxs[0] - NewValueStartOffset;
						while(true){
							if (datalen > 0) {
								MsColumn msColumn = mlrd.table.getSorted_VariantColumns()[ColIdx];
								byte[] sval = value_glea.read(datalen);
								String TmpStr = MsFunc.BuildSegment(msColumn, sval);
								res.NewValues.add(TmpStr);
							}
							if (value_glea.available() > 0) {
								datalen = idxs[ColIdx + 1] - idxs[ColIdx];
								ColIdx += 1;
							}else{
								break;
							}
						}
					}			
				}
			}
		}
		
		if (includeVarCol || UnLocalValCol.size()>0) {
			getFullUpdateDataByPrimaryKey(mlrd, res, includeVarCol, UnLocalValCol.toArray(new MsColumn[0]));
		}

		InitUpdatePrimarykey(mlrd, res);
		return res;
	}
	
	private DBOptUpdate PriseUpdateLog_LOP_MODIFY_ROW(MsLogRowData mlrd) {
		DBOptUpdate res = new DBOptUpdate();
		res.tableName = mlrd.table.GetFullName();
		res.obj_id = mlrd.table.id;
		if (mlrd.r2==null || mlrd.r2.length==0) {
			//TODO:!!!!!!!!短嗤麼囚��楹楹楹
			logger.warn("方象垂��"+md.Db.GetFullDbName()+"��燕:"+ mlrd.table.GetFullName()+"涙麼囚�仝�仟荷恬函���。。。�");
			return null;
		}else{
			int nullMapLength = (mlrd.table.getNullMapSorted_Columns().length + 7) >>> 3;
			int varDataIdxOffset = mlrd.table.theFixedLength + nullMapLength + 2;
			if (mlrd.offset > varDataIdxOffset) {
				//しまった、ここに秘ってなら。リアデ�`タを�iむ
				if(!getFullUpdateDataByPrimaryKey(mlrd, res, true)){
					return null;
				}
			}else if(mlrd.offset < mlrd.table.theFixedLength){
				//俐個袈律頁耕協海業忖粁
			    for (MsColumn mColumn : mlrd.table.getNullMapSorted_Columns()) {
					if (mColumn.leaf_pos > 0) {
						if (mlrd.offset == mColumn.theRealPosition) {
							//孀欺双
							String TmpStr = MsFunc.BuildSegment(mColumn, mlrd.r0);
							res.OldValues.add(TmpStr);
							TmpStr = MsFunc.BuildSegment(mColumn, mlrd.r1);
							res.NewValues.add(TmpStr);
							
							InitUpdatePrimarykey(mlrd, res);
						}else if (mlrd.offset > mColumn.theRealPosition && mlrd.offset < mColumn.theRealPosition + mColumn.max_length){
						    //壓双峙袈律坪�。�俶勣恵諒方象垂臥孀寔糞方象
							if(!getFullUpdateDataByPrimaryKey(mlrd, res, false, mColumn)){
								return null;
							}
							break;
						}
					}
				}
			}else{
				//顕固阻nullMap
				//PriseMixedUpdateBlock(mlrd, res, true, nullMapLength, mlrd.offset, mlrd.r0);  //old方象短焚担喘~音勣阻
				//PriseMixedUpdateBlock(mlrd, res, false, nullMapLength, mlrd.offset, mlrd.r1);
				
				int OverlapNullMapLen = nullMapLength - (mlrd.offset - mlrd.table.theFixedLength);
				if (mlrd.r1 != null && mlrd.r1.length >= OverlapNullMapLen) {
					GenericLittleEndianAccessor glea = new GenericLittleEndianAccessor(mlrd.r1);
					glea.skip(OverlapNullMapLen);//柳狛顕固議nullMap
					if (glea.available()>0) {
						//沫哈翠寄弌,宸倖頁方象贋刈議variant忖粁沫哈燕
						int idxsCount = glea.readShort();
						short[] idxs = new short[idxsCount];
						for (int i = 0; i < idxsCount; i++) {
							if (glea.available() < 2) {
								break;
							}
							idxs[i] = glea.readShort();
						}
						
						//PriseValues(mlrd, res, false, nullMapLength, idxs, glea);
						if (glea.available()>0) {
							//柴麻蝕兵厚仟議双��阿欺厚仟議曾倖双頁慧欺匯翠戦中議
							int ColIdx = 0;
							int datalen = idxs[0] - (mlrd.offset + OverlapNullMapLen + 2 + idxs.length * 2);
							while(true){
								if (datalen > 0) {
									MsColumn msColumn = mlrd.table.getSorted_VariantColumns()[ColIdx];
									byte[] sval = glea.read(datalen);
									String TmpStr = MsFunc.BuildSegment(msColumn, sval);
									res.NewValues.add(TmpStr);
								}
								if (glea.available() > 0) {
									datalen = idxs[ColIdx + 1] - idxs[ColIdx];
									ColIdx += 1;
								}else{
									break;
								}
							}
						}
					}
				}

				InitUpdatePrimarykey(mlrd, res);
			}
		}
		return res;
	}
	
	private void InitUpdatePrimarykey(MsLogRowData mlrd, DBOptUpdate res){
		if (res.KeyField!=null && res.KeyField.size() > 0) {
			return;
		}
		
		GenericLittleEndianAccessor glea_key = new GenericLittleEndianAccessor(mlrd.r2);
		byte prefix = glea_key.readByte();
		//16協海麼囚双��36延海麼囚双
		if (prefix!=0x16 && prefix!=0x36) {
			logger.error("厚仟晩崗少御��"+mlrd.table.GetFullName()+"r2念弸呟械�。。。。�LSN:" + mlrd.LSN);
		}
		boolean fstbcl = true;
		//TODO:麼囚蛍苧音嬬葎null��葎焚担珊嗤nullMap�殖殖殖�
		int nullMapLength = (mlrd.table.getSorted_PrimaryColumns().length + 7) >>> 3;
		short[] idxs = null;
		int varColPosition = 0;
		for (MsColumn mColumn : mlrd.table.getSorted_PrimaryColumns()) {
			byte[] datas;
			if (mColumn.leaf_pos > 0) {
				//協海双
				datas = glea_key.read(mColumn.max_length);
			}else{
				//延海双
				if (fstbcl) {
					fstbcl = false;
					//及匯倖延海双念中氏嗤現紗方象
					int columnCount = glea_key.readShort();
					byte[] nullMap = glea_key.read(nullMapLength);
					int idxsCount = glea_key.readShort();
					idxs = new short[idxsCount];
					for (int j = 0; j < idxsCount; j++) {
						idxs[j] = glea_key.readShort();
					}
				}				
				short dataEndOffset = idxs[varColPosition];
				if (dataEndOffset<0) {
					logger.error("厚仟晩崗少御��"+mlrd.table.GetFullName()+"r2念弸呟械�。。。�LSN:" + mlrd.LSN);
				}
				int datalen = dataEndOffset - glea_key.getBytesRead();
				datas = glea_key.read(datalen);
				varColPosition++;
			}
			String TmpStr = MsFunc.BuildSegment(mColumn, datas);
			res.KeyField.add(TmpStr);
		}
	}
	/**
	 * 資函侭嗤議延海双
	 * @param mTable
	 * @return
	 */
	private String getFullUpdateDataVarFields(MsTable mTable, MsColumn... itemList){
		String res = ""; 
		for (MsColumn msColumn : mTable.GetFields()) {
			if (!isSkipColType(msColumn)) {
				if (msColumn.leaf_pos < 0) {
					boolean containsInList = false;
					if (itemList!=null && itemList.length>0) {
						for (MsColumn item : itemList) {
							if (item.equals(msColumn)) {
								containsInList = true;
								break;
							}
						}
					}
					if (!containsInList) {
						res+= ",["+msColumn.Name+"]";
					}
				}
			}
		}
		if (!res.isEmpty()) {
			return res.substring(1);
		}else 
			return "";
	}
	
	@SuppressWarnings("unused")
	@Deprecated
	private boolean getRowDataFromDbcc(MsLogRowData mlrd) {
		//!!!音嬬宥狛dbcc肇資函圻兵方象!!!!
		//厮岑3嶽秤趨氏誼欺危列方象
		//1.泌惚圻兵佩瓜評茅��資函議繍頁凪万佩方象
		//2.泌惚亟秘阻詔栽双��Slot繍瓜嶷仟電會��誼欺議匆氏頁危列峙��
		//3.泌惚燕瓜嶷秀.......方象辛嬬氏瓜蛍塘欺凪万匈
		
//		try {
//			Statement statement = md.Db.conn.createStatement();
//			String Sql = String.format("dbcc page (%s,%d,%d,1)with tableresults", md.getDB().dbName, mlrd.pageFID, mlrd.pagePID);
//			ResultSet Rs = statement.executeQuery(Sql);
//			while (Rs.next()) {
//			}
//			Rs.close();
//			statement.close();
//			return true;
//		} catch (SQLException e) {
//			logger.error("響函方象垂圻兵方象払移��" + mlrd.LSN, e);
//		}
		return false;
	}
	
	private String getrsColValueString(ResultSetMetaData rsmd, ResultSet Rs, int i) throws SQLException{
		String TStr = "["+rsmd.getColumnName(i)+"]=";
		Rs.getObject(i);
		if (Rs.wasNull()) {
			TStr += "NULL";
		}else{
			switch (rsmd.getColumnType(i)) {
			case java.sql.Types.BIT:
			case java.sql.Types.BOOLEAN:
				TStr += Rs.getBoolean(i)?"1":"0";
				break;
			case java.sql.Types.TINYINT:
			case java.sql.Types.SMALLINT:
			case java.sql.Types.INTEGER:
			case java.sql.Types.BIGINT:
				TStr += Rs.getInt(i);
				break;
			case java.sql.Types.FLOAT:
			case java.sql.Types.REAL:
			case java.sql.Types.DOUBLE:
			case java.sql.Types.NUMERIC:
			case java.sql.Types.DECIMAL:
				TStr += Rs.getDouble(i);
				break;
			case java.sql.Types.CHAR:
			case java.sql.Types.VARCHAR:
			case java.sql.Types.LONGVARCHAR:
			case java.sql.Types.NCHAR:
			case java.sql.Types.NVARCHAR:
			case java.sql.Types.LONGNVARCHAR:
				TStr += "'" + Rs.getString(i) + "'";
				break;	
			case java.sql.Types.DATE:
				TStr += "'" + Rs.getDate(i).toString() + "'";
				break;
			case java.sql.Types.TIME:
				TStr += "'" + Rs.getTime(i).toString()+ "'";
				break;
			case java.sql.Types.TIMESTAMP:
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
				TStr += "'" + dateFormat.format(Rs.getTimestamp(i).getTime())+"'";
				break;
			case -155:
				TStr += "'" + Rs.getTimestamp(i).toString();
				int tzo = Rs.getTimestamp(i).getTimezoneOffset();
				if (tzo<0) {
					TStr += " +"+Math.abs(tzo)/60+":"+Math.abs(tzo)%60;
				}else{
					TStr += " -"+Math.abs(tzo)/60+":"+Math.abs(tzo)%60;
				}
				TStr +="'";
				break;
			case java.sql.Types.BINARY:
			case java.sql.Types.VARBINARY:
			case java.sql.Types.LONGVARBINARY:
			case java.sql.Types.NULL:
			case java.sql.Types.OTHER:
			case java.sql.Types.JAVA_OBJECT:
			case java.sql.Types.DISTINCT:
			case java.sql.Types.STRUCT:
			case java.sql.Types.ARRAY:
			case java.sql.Types.BLOB:
			case java.sql.Types.CLOB:
			case java.sql.Types.REF:
			case java.sql.Types.DATALINK:
			case java.sql.Types.ROWID:
			case java.sql.Types.NCLOB:
			case java.sql.Types.SQLXML:
			case java.sql.Types.REF_CURSOR:
			case java.sql.Types.TIME_WITH_TIMEZONE:
			case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
				TStr += "0x" + HexTool.toString(Rs.getBytes(i)).replace(" ", "");
				break;
			default:
				throw new UnsupportedOperationException("Not supported yet.530");
			}
		}
		return TStr;
	}
	
	/**
	 * 
	 * 宥狛select恵諒方象垂��資函双恷仟峙
	 * @param mlrd
	 * @param res
	 * @param includeVarColumn 頁倦淫根延海双
	 * @param mColumn 勣厚仟議協海双鹿栽
	 * @return
	 */
	private boolean getFullUpdateDataByPrimaryKey(MsLogRowData mlrd, DBOptUpdate res, boolean includeVarColumn, MsColumn... mColumn){
		InitUpdatePrimarykey(mlrd,res);
		String WhereKey = "";
		for (String string : res.KeyField) {
			WhereKey += " and " + string;
		}
		WhereKey = WhereKey.substring(5);
		
		String SelectFields = includeVarColumn?getFullUpdateDataVarFields(mlrd.table, mColumn):"";
		for (MsColumn item : mColumn) {
			if(SelectFields.isEmpty()){
				SelectFields += item.getSafeName();
			}else{
				SelectFields += "," + item.getSafeName();
			}
		}
		
		try {
			Statement statement = md.Db.conn.createStatement();
			String Sql = String.format("SELECT %s FROM %s WHERE %s", SelectFields, mlrd.table.GetFullName(), WhereKey);
			ResultSet Rs = statement.executeQuery(Sql);
			if (!Rs.next()) {
				logger.error("資函厚仟方象払移�｛佇�象音贋壓��LSN:" + mlrd.LSN);
				return false;
			}
			ResultSetMetaData rsmd = Rs.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++) {
				String TStr = getrsColValueString(rsmd, Rs, i);
				res.NewValues.add(TStr);
			}
			Rs.close();
			statement.close();
			return true;
		} catch (SQLException e) {
			logger.error("響函方象垂晩崗払移��", e);
		}
		return false;
		
	}
	
	public byte[] get_LCX_TEXT_MIX_DATA(int key1,int key2,int pageFID,int pagePID,int slotid, MsLogRowData mlrd, int dlen) {
		String key = pageFID + ":" + pagePID + ":" + slotid;
		byte[] data = LCX_TEXT_MIX.get(key);
		if (data == null) {
			logger.error("晩崗盾裂払移�〇馮� LCX_TEXT_MIX_DATA 払移 LSN" + mlrd.LSN);
			return null;
		}
		GenericLittleEndianAccessor TEXT_MIX_DATA = new GenericLittleEndianAccessor(data);
		TEXT_MIX_DATA.seek(4, SeekOrigin.soFromBeginning);
		int datakey1 = TEXT_MIX_DATA.readInt();
		int datakey2 = TEXT_MIX_DATA.readInt();
		if (datakey1 == key1 && datakey2 == key2) {
			int dataType = TEXT_MIX_DATA.readShort();
			switch (dataType) {
			case 0:
				//方象海業 + 00 
				int dataLength0 = TEXT_MIX_DATA.readInt();
				int UNKNOWN = TEXT_MIX_DATA.readShort();
				return TEXT_MIX_DATA.read(dataLength0);
			case 3:
				//朔中諸効議畠頁方象坪否
				return TEXT_MIX_DATA.read(dlen);
			case 5:
				//珊頁朕村
				TEXT_MIX_DATA.seek(0x18, SeekOrigin.soFromBeginning);
				int dataLength5 = TEXT_MIX_DATA.readInt();
				
				int datapagePID = TEXT_MIX_DATA.readInt();
				int datapageFID = TEXT_MIX_DATA.readShort();
				int dataslotid = TEXT_MIX_DATA.readShort();
				return get_LCX_TEXT_MIX_DATA(key1,key2,datapageFID,datapagePID,dataslotid,mlrd, dataLength5);
			default:
				throw new UnsupportedOperationException("Not supported yet.");
			}
		}else{
			throw new UnsupportedOperationException("Not supported yet. ERROR Key1 and Key2");
		}
	}
	public byte[] get_LCX_TEXT_MIX_DATA(byte[] idxdata, MsLogRowData mlrd) {
		GenericLittleEndianAccessor TEXT_MIX_IDX = new GenericLittleEndianAccessor(idxdata);
		int key1 = TEXT_MIX_IDX.readInt();
		int key2 = TEXT_MIX_IDX.readInt();
		int pagePID = TEXT_MIX_IDX.readInt();
		int pageFID = TEXT_MIX_IDX.readShort();
		int slotid = TEXT_MIX_IDX.readShort();
		return get_LCX_TEXT_MIX_DATA(key1, key2, pageFID, pagePID, slotid, mlrd, 0);
	}
	
	public DBOptInsert PriseInsertLog_LOP_INSERT_ROWS(MsLogRowData mlrd) {
		// 	   | 30 00  | 08 00 |..............	| 04 00 |	........	| 02 00 | 00 00 |...............|
		//海業   |		4	|	4	|		x		|	4	|		x		|	4	|2*x	|		x		|
		//     |	��	|	��	|		��		|	∠	|		⊥		|	⌒ 	|	∂	|		∇		|≡
		// ��:晩崗窃侏
		// ��:狼由窃侏峙 方象潤崩了崔�│楜�offset��
		// ��:狼由窃侏峙 方象
	    // ∠:悳双方
		// ⊥:nullMap 耽1bit燕幣1双頁倦葎null
		// ⌒:徭協吶忖粁峙双方
		// ∂:耽2忖准炎紛匯倖��徭協吶忖粁方象議潤崩了崔
		// ∇:徭協吶忖粁方象
		// ≡��
		// |||||||
		DBOptInsert res = new DBOptInsert();
		res.obj_id = mlrd.obj_id;
		res.tableName = mlrd.table.GetFullName();
		
		MsColumn[] mcs = mlrd.table.getNullMapSorted_Columns();
		
		GenericLittleEndianAccessor glea = new GenericLittleEndianAccessor(mlrd.r0);
		int LogType = glea.readShort();
		if ((LogType&0x06)>0) {
			logger.warn("==================氏払移議方象====================");
			logger.warn(HexTool.toString(mlrd.r0));
			return null;
		}
		
		/*if (LogType!=0x30) {
			md.GetOutPut().Error("嘆貌音頁Insert晩崗��"+LSN);
			return false;
		}
		*/
		int inSideDataOffset = glea.readShort();  //狼由窃侏峙 方象潤硫
		glea.seek(inSideDataOffset, SeekOrigin.soFromBeginning);
		int ColumnCount = glea.readShort();  //輝念燕双方
		/*if (ColumnCount != mcs.length) {
			//燕評茅双岻朔��晩崗戦珊頁參念議方楚
			logger.error("双方嚥晩崗音謄塘!" + mlrd.LSN);
			return null;
		}*/
		int boolbit = 0;
		int NullMapLength = (ColumnCount + 7) >>> 3;
		byte[] NullMap = glea.read(NullMapLength);
		int ExtDataCount = 0;
		if (glea.available() > 2) {
			ExtDataCount = glea.readShort();  //制婢方象方楚
		}
		short[] ExtDataIdxList = new short[ExtDataCount];
		for (int i = 0; i < ExtDataCount; i++) {
			ExtDataIdxList[i] = glea.readShort();
		}
		int ExtDataBaseOffset = glea.getBytesRead();
		for (int i = 0; i < mcs.length; i++) {
			MsColumn mc = mcs[i];
			if (!mc.IsDefinitionColumn) {
				if (mc.is_nullable) {
					//登僅双頁倦葎null
					int a = mc.nullmap >>> 3;
					int b = mc.nullmap & 7;
					if(((NullMap[a] & 0xFF) & (1 << b)) > 0){
						//  cell is null
						continue;
					}
				}

				if (mc.leaf_pos < 0) {
					int idx = 0 - mc.leaf_pos - 1;
					if (idx < ExtDataIdxList.length) {
						int dataBegin;
						if (idx == 0) {
							dataBegin = ExtDataBaseOffset;
						}else{
							dataBegin = ExtDataIdxList[idx - 1] & 0x7FFF;
						}
						int datalen = (ExtDataIdxList[idx] & 0x7FFF) - dataBegin;
						if (datalen <= 0 || datalen > glea.available()) {
							//腎忖憲堪
							res.Fields.add(mc);
							res.Values.add(new byte[0]);
						}else{
							glea.seek(dataBegin, SeekOrigin.soFromBeginning);
							byte[] tmp;
							
							if ((ExtDataIdxList[idx] & 0x8000) > 0) {
								//泌惚恷互了頁1傍苧方象壓LCX_TEXT_MIX淫嶄
								tmp = glea.read(0x10);
								
								tmp = get_LCX_TEXT_MIX_DATA(tmp, mlrd);								
							}else{
								tmp = glea.read(datalen);
							}
							res.Fields.add(mc);
							res.Values.add(tmp);
						}
					}
				}else{
					if (isSkipColType(mc)) {
						continue;
					}
					//蝕兵響函方象
					glea.seek(mc.leaf_pos, SeekOrigin.soFromBeginning);
					byte[] tmp = glea.read(mc.max_length);
					
					if (mc.type_id == MsTypes.BIT) {
						if (((1 << boolbit) & (tmp[0] & 0xFF)) > 0) {
							tmp[0] = 1;
						}else{
							tmp[0] = 0;
						}										
						boolbit++;
						if (boolbit == 8) {
							boolbit = 0;
						}
					}
					res.Fields.add(mc);
					res.Values.add(tmp);
				}
			}
		}
		return res;
	}
	
	private boolean isSkipColType(MsColumn mc){
		switch (mc.type_id) {
			case MsTypes.UNIQUEIDENTIFIER:
			case MsTypes.HIERARCHYID:
			case MsTypes.GEOMETRY:
			case MsTypes.GEOGRAPHY:
			case MsTypes.SQL_VARIANT:
			case MsTypes.XML:
			case MsTypes.TIMESTAMP://宸嶽窃侏議峙音嬬岷俊亟秘方象垂
			return true;
		default:
			return false;
		}
	}
	
	/**
	 * 嬬校恬葎where訳周議忖粁窃侏
	 * @param mc
	 * @return
	 */
	private boolean canBeWhereSegColType(MsColumn mc){
		switch (mc.type_id) {
		case MsTypes.DATE:
		case MsTypes.TIME:
		case MsTypes.DATETIME2:
		case MsTypes.DATETIMEOFFSET:
		case MsTypes.TINYINT:
		case MsTypes.SMALLINT:
		case MsTypes.INT:
		case MsTypes.BIGINT:
		case MsTypes.SMALLDATETIME:
		case MsTypes.DATETIME:
		case MsTypes.BIT:
		case MsTypes.DECIMAL:
		case MsTypes.NUMERIC:
		case MsTypes.MONEY:	
		case MsTypes.SMALLMONEY:
		case MsTypes.VARCHAR:
		case MsTypes.CHAR:
		//case MsTypes.TEXT:  //埋隼辛參恬葎訳周��徽頁宸倖忖粁坪否曳熟謹��音芦畠
		//case MsTypes.NTEXT:
		//case MsTypes.REAL:  //坪否葎音芦畠議＾除貌￣峙.
		//case MsTypes.FLOAT:	
		case MsTypes.NVARCHAR:
		case MsTypes.NCHAR:
		case MsTypes.SYSNAME:

			return true;
		default:
			return false;
		}
	}
}
