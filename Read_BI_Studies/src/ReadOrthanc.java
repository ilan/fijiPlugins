import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author ilan
 */
public class ReadOrthanc {
	ReadBIdatabase parent;
	ArrayList<orth1Study> orthStudies = null;
	ReadOrthancSub orthSub;

	ReadOrthanc(ReadBIdatabase par1){
		parent = par1;
		int j,k;
		String ipAddr, tmp, port, url = null;
		j = par1.AETitle.indexOf("@");
		k = par1.AETitle.indexOf(":");
		if( j>0 && k>j) {
			ipAddr = par1.AETitle.substring(j+1, k+1);
			tmp = par1.m_dataPath;
			j = tmp.indexOf(ipAddr);
			port = "8042";
			if( j>0) {
				port = tmp.substring(j+ipAddr.length());
				k = port.indexOf("/");
				if( k>0) port = port.substring(0, k);
			}
			url = "http://" + ipAddr + port + "/";
		}
		orthSub = new ReadOrthancSub(url, par1.userPw);	// located in PetCt jar
	}

	@SuppressWarnings("unchecked")
	void setOrthSdy(Object studies) {
		orthStudies = null;
		if( studies == null) return;
		if( studies instanceof ArrayList) orthStudies = (ArrayList) studies;
	}

	class orth1Study {
		String patName = null, patID = null, studyName = null;
		String  accession = null, uuid1 = null;
		Date studyDate = null;
		JSONArray seriesList = null;
	}

	boolean isEqual(String in1, String in2) {
		if( in1 == null && in2 == null) return true;
		if( in1 == null) return false;
		return in1.equals(in2);
	}

	void fillTable ( int indx, DefaultTableModel mod1, boolean readFlg) {
		int i, j, k, n, n1;
		int type1 = 0;	// has value 0 or 2 (for delete)
		String uuid0, uuid1, uuid2;
		String patName, patID, tmp1, tmp0;
		String dPatName = null, dStyName = null;
		orth1Study currStudy;
		Date startDate, stopDate;
		boolean isID = false;
		JSONArray patients, studies, series;
		JSONObject patient, mainp, study, mains, ser1, main1;
		try {
			if( indx > 2) {
				IJ.log("\nOnly patient name/id, date and study are supported in Orthanc.\n");
				return;
			}
			if( !readFlg) {
				type1 = 2;
			}
			startDate = parent.getDate(type1);
			stopDate = parent.getDate(type1+1);
			switch(indx) {
				case 0:
					dPatName = parent.getDlgPatName(type1).trim().toLowerCase();
					isID = parent.isID(dPatName);
					if( isID) dPatName = ChoosePetCt.compressID(dPatName);
					break;

				case 2:
					dStyName = parent.getDlgPatName(type1).trim().toLowerCase();	// same position for study
					break;
			}
			orthStudies = new ArrayList<orth1Study>();
			Object[] row1 = new Object[ReadBIdatabase.TBL_BF+2];
			patients = (JSONArray) orthSub.ReadJson("patients");
			for(i = n1 = 0; i < patients.size(); i++) {
				uuid0 = (String) patients.get(i);
				patient = (JSONObject) orthSub.ReadJson("patients/" + uuid0);
				mainp = (JSONObject) patient.get("MainDicomTags");
				patName = (String) mainp.get("PatientName");
				if( patName == null) continue;
				patID = (String) mainp.get("PatientID");
				if( indx == 0) {
					if( isID) {
						tmp1 = ChoosePetCt.compressID(patID);
						if( !tmp1.equals(dPatName)) continue;
					} else {
						tmp1 = patName.trim().toLowerCase();
						if( !tmp1.startsWith(dPatName)) continue;
					}
				}
				studies = (JSONArray) patient.get("Studies");
				for( j=0; j <  studies.size(); j++) {
					currStudy = new orth1Study();
					currStudy.uuid1 = uuid1 = (String) studies.get(j);
					currStudy.patName = patName;
					currStudy.patID = patID;
					study = (JSONObject) orthSub.ReadJson("studies/" + uuid1);
					mains = (JSONObject) study.get("MainDicomTags");
					tmp1 = (String) mains.get("StudyDate");
					tmp0 = (String) mains.get("StudyTime");
					currStudy.studyDate = ChoosePetCt.getDateTime(tmp1, tmp0);
					if( currStudy.studyDate.before(startDate)|| currStudy.studyDate.after(stopDate)) continue;
					tmp1 = (String) mains.get("StudyDescription");
					if( tmp1 == null) tmp1 = "";
					currStudy.studyName = tmp1;
					if( indx == 2) {
						tmp1 = currStudy.studyName.toLowerCase();
						if( !tmp1.startsWith(dStyName)) continue;
						}
					tmp1 = (String) mains.get("AccessionNumber");
					currStudy.accession = tmp1;
					currStudy.seriesList = series = (JSONArray) study.get("Series");
					orthStudies.add(currStudy);
					if( mod1 == null) continue;
					row1[ReadBIdatabase.TBL_PAT_NAME] = patName;
					row1[ReadBIdatabase.TBL_PAT_ID] = patID;
					row1[ReadBIdatabase.TBL_DATE] = currStudy.studyDate;
					row1[ReadBIdatabase.TBL_STUDY] = currStudy.studyName;
					row1[ReadBIdatabase.TBL_ACCESSION] = tmp1;
					row1[ReadBIdatabase.TBL_SERIES] = null;
					row1[ReadBIdatabase.TBL_BF] = false;
					n = series.size();
					for( k=0; k<n; k++) {	// get brown fat
						uuid2 = (String) series.get(k);
						ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid2);
						main1 = (JSONObject) ser1.get("MainDicomTags");
						tmp1 = (String) main1.get("SeriesDescription");
						if( tmp1 != null && tmp1.startsWith("Brown fat")) {
							row1[ReadBIdatabase.TBL_BF] = true;
							break;
						}
					}
					if( n == 1 || !readFlg) for( k=0; k<n; k++){
						uuid2 = (String) series.get(k);
						ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid2);
						main1 = (JSONObject) ser1.get("MainDicomTags");
						tmp1 = (String) main1.get("SeriesDescription");
						if(tmp1==null ||tmp1.isEmpty()) tmp1 = "-";
						row1[ReadBIdatabase.TBL_SERIES] = tmp1;
						tmp1 = (String) main1.get("SeriesInstanceUID");
						row1[ReadBIdatabase.TBL_SER_UID] = tmp1;
						if( k<n-1) mod1.addRow(row1);
					}
					mod1.addRow(row1);
					if( n1++ > 300 && indx != 1) {
						JOptionPane.showMessageDialog(parent,
							"Too many series to display...\n" +
							"Try to limit the search with a more detailed patient name.");
						return;
					}
				}
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	boolean checkSeriesOk(ImagePlus img1, String serName) {
		JSONArray series;
		JSONObject ser1, main1;
		String tmp1, uuid2;
		int j;
		series = orthSub.findSeries(img1);
		if( series == null) return false;
		for( j =0; j <  series.size(); j++) {
			uuid2 = (String) series.get(j);
			ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid2);
			main1 = (JSONObject) ser1.get("MainDicomTags");
			tmp1 = (String) main1.get("SeriesInstanceUID");
			if( orthSub.currSeriesUID.equals(tmp1)) {
				IJ.log("Study already written: " + orthSub.currPatName + "  " + serName);
				return false;
			}
		}
		return true;
	}

	void expandOrthMouse(int row, DefaultTableModel mod1) {
		String sdyDesc = (String) mod1.getValueAt(row, ReadBIdatabase.TBL_STUDY);
		Date styDate = (Date) mod1.getValueAt(row, ReadBIdatabase.TBL_DATE);
		String mrn = (String) mod1.getValueAt(row, ReadBIdatabase.TBL_PAT_ID);
		boolean bfat = (Boolean) mod1.getValueAt(row, ReadBIdatabase.TBL_BF);
		orth1Study currStudy;
		JSONArray series;
		JSONObject ser1, main1;
		String uuid1, tmp1, tmp2;
		int i, j, n = orthStudies.size();
		boolean found1 = false;
		try {
			for( i=0; i<n; i++) {
				currStudy = orthStudies.get(i);
				if(!isEqual(currStudy.patID, mrn) || !ChoosePetCt.isSameDate( currStudy.studyDate,styDate)
					|| !isEqual(currStudy.studyName, sdyDesc)) continue;
				series = currStudy.seriesList;
				found1 = true;
				Object[] row1 = new Object[ReadBIdatabase.TBL_BF+2];
				row1[ReadBIdatabase.TBL_PAT_NAME] = currStudy.patName;
				row1[ReadBIdatabase.TBL_PAT_ID] = mrn;
				row1[ReadBIdatabase.TBL_DATE] = styDate;
				row1[ReadBIdatabase.TBL_STUDY] = sdyDesc;
				row1[ReadBIdatabase.TBL_ACCESSION] = currStudy.accession;
				row1[ReadBIdatabase.TBL_BF] = bfat;
				for( j=0; j<series.size(); j++) {
					uuid1 = (String) series.get(j);
					ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid1);
					main1 = (JSONObject) ser1.get("MainDicomTags");
					tmp1 = (String) main1.get("SeriesDescription");
					if(tmp1==null ||tmp1.isEmpty()) tmp1 = "-";
					row1[ReadBIdatabase.TBL_SERIES] = tmp1;
					tmp2 = (String) main1.get("SeriesInstanceUID");
					row1[ReadBIdatabase.TBL_SER_UID] = tmp2;
					if( j==0) {
						mod1.setValueAt(tmp1, row, ReadBIdatabase.TBL_SERIES);
						mod1.setValueAt(tmp2, row, ReadBIdatabase.TBL_SER_UID);
					}
					else mod1.insertRow(j+row, row1);
				}
				break;
			}
			if( !found1) parent.maybeRereadTable();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	void readStudy(Object[] row1) {
		int i, j, read, nSlice, n;
		double progVal;
		File outFile1;
		FileOutputStream flOut1;
		FileInfo fi;
		int width = -1, height = 0, depth=0;
		ImagePlus imp;
		Calibration cal;
		int mode = ReadBIdatabase.READ_IF_EXIST;
		orth1Study currStudy;
		String uuid1, uuid2, uri, ReadCDPath, path1, path2, sliceName, serUID;
		String path4=null, lastGood=null, modality, tmp1, tmp2, info, serName, dup1;
		String label1;
		Opener opener = null;
		boolean isConference, isExist, isCompressed;
		byte[] buf;
		String[] dupName;
		BI_dbSaveInfo biSave;
//		ShortProcessor slice;
		ImageStack stack;
		ImagePlus img1;
		JSONArray series, instances;
		JSONObject ser1, main1, tags, transfer;
		try {
			if( (isConference = parent.isToggleMode())) mode = ReadBIdatabase.BOTH_READ_AND_WRITE;
			serName  = (String) row1[ReadBIdatabase.TBL_SERIES];
			if( serName != null && serName.equals("-")) serName = "";	// special case
			serUID = (String) row1[ReadBIdatabase.TBL_SER_UID];
			currStudy = getCurStudy(row1);
			if( currStudy == null) return;
			outFile1 = File.createTempFile("dicom", "dcm");
			path1 = outFile1.getPath();
			outFile1.delete();
			SimpleDateFormat df1 = new SimpleDateFormat("yyMMddHHmm");
			path2 = File.separator + currStudy.studyName + df1.format(currStudy.studyDate);
			path1 += path2.replaceAll("[: -]", "");
			series = currStudy.seriesList;
			dupName = getDuplicates(series);
			n = series.size();
			for( i=0; i<n; i++) {
				biSave = getBIsave(currStudy);
				dup1 = "";	// nothing
				if( dupName != null) dup1 = dupName[i];
				uuid1 = (String) series.get(i);
				ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid1);
				instances = (JSONArray) ser1.get("Instances");
				nSlice = instances.size();
				main1 = (JSONObject) ser1.get("MainDicomTags");
				modality = (String) main1.get("Modality");
				tmp1 = (String) main1.get("SeriesDescription");
				if(serName != null && tmp1 != null && !serName.equals(tmp1)) continue;
				if(tmp1 == null || tmp1.isEmpty()) tmp1 = "a";
				if(serName != null &&  serUID != null) {
					tmp2 = ((String) main1.get("SeriesInstanceUID")).trim();
					if( !tmp2.equals(serUID.trim())) continue;
				}
				biSave.serName = tmp1;
				tmp1 = tmp1.replaceAll("[:/ -]", "");	// here "/" is dropped as well
				path2 = path1 + File.separator + tmp1 + dup1;
				ReadCDPath = parent.CopyCDFiles(path2, null, mode);
				if( !isConference) {
					outFile1 = new File(path2);
					if( !outFile1.exists()) outFile1.mkdirs();
				}
				stack = null;
				isCompressed = false;
				for( j=0; j<nSlice; j++) {
					progVal = (j + 1.0)/nSlice;
					IJ.showProgress(progVal);
					sliceName = File.separator + modality + String.format("%04d", j);
					isExist = false;
					if( ReadCDPath != null) {
						path4 = ReadCDPath + sliceName;
						outFile1 = new File(path4);
						isExist = outFile1.exists();
					}
					if( !isExist && !isConference) {
						path4 = path2 + sliceName;
						outFile1 = new File(path4);
					}
					if( !outFile1.exists()) {
						uuid2 = (String) instances.get(j);
						if( j==0) {
							tags = (JSONObject) orthSub.ReadJson("instances/" + uuid2 + "/header?simplify");
							tmp1 = (String) tags.get("TransferSyntaxUID");
							if( tmp1 != null && tmp1.startsWith("1.2.840.10008.1.2.4.")) {
								isCompressed = true;
							}
						}
						if( isCompressed) {
							stack = readCompressed(uuid2, stack, j+1);
							if( stack == null) break;
						} else {
							flOut1 = new FileOutputStream(outFile1);
							uri = orthSub.baseURL + "instances/" + uuid2 + "/file";
							buf = new byte[16384];
							InputStream stream = orthSub.OpenUrl(uri, null);
							while( (read = stream.read(buf))>0) {
								flOut1.write(buf, 0, read);
							}
							flOut1.close();
							if( !isConference) outFile1.deleteOnExit();
						}
					}
					if( isCompressed) continue;
					opener = new Opener();
					opener.setSilentMode(true);
					imp = opener.openImage(path4);
					if( imp == null) continue;
					if( stack==null) {
						width = imp.getWidth();
						height = imp.getHeight();
						depth = imp.getStackSize();
						ColorModel cm = imp.getProcessor().getColorModel();
						stack = new ImageStack(width, height, cm);
					}
					if( (depth > 1 && nSlice > 1) || width != imp.getWidth() || height != imp.getHeight()) {
						imp.show();	// show a normal stack
						parent.imgList.add(imp);
						stack = null;
						depth = 0;
						continue;
					}
					info = (String)imp.getProperty("Info");
					label1 = null;
					if (depth == 1) {
						label1 = imp.getTitle();
						if (info!=null)
							label1 += "\n" + info;
					}
					ImageStack inputStack = imp.getStack();
					for (int slice=1; slice<=inputStack.getSize(); slice++) {
						ImageProcessor ip = inputStack.getProcessor(slice);
			//			if (ip.getMin()<min) min = ip.getMin();
			//			if (ip.getMax()>max) max = ip.getMax();
						stack.addSlice(label1, ip);
					}
					lastGood = path4;
				}
				if( stack == null ) return;
				if( isCompressed) {
					stack = ChoosePetCt.mySort(stack);
					info = stack.getSliceLabel(1);
					img1 = new ImagePlus(parent.getTitleInfo(info, null), stack);
					extractCalibration(img1, info);
				} else {
					if( opener == null) return;
					img1 = opener.openImage(lastGood);
					fi = img1.getOriginalFileInfo();

					info = (String)img1.getProperty("Info");
					cal = img1.getCalibration();
					stack = ChoosePetCt.mySort(stack);
					if( fi != null) {
						fi.fileFormat = FileInfo.UNKNOWN;
						fi.fileName = "";
						fi.directory = path2;
						if( isConference) fi.directory = ReadCDPath;
					}
					img1 = new ImagePlus(parent.getTitleInfo(info, null), stack);
					img1.setFileInfo(fi);
					img1.setCalibration(cal);
				}
				img1.setProperty("bidb", biSave);	// save database info
				if( nSlice == 1) img1.setProperty("Info", info);
				parent.myMakeMontage(img1, info, false);
				parent.imgList.add(img1);
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	ImageStack readCompressed(String uuid, ImageStack stack, int indx) {
		try {
			String uri = orthSub.baseURL + "instances/" + uuid + "/image-uint16";
			BufferedImage bi = ImageIO.read( orthSub.OpenUrl(uri, "image/png"));
			if( bi.getType() != BufferedImage.TYPE_USHORT_GRAY) {
				IJ.showProgress(1.0);
				IJ.log("Compressed Dicom files not read");
				return null;
			}
			ImageProcessor slice = new ShortProcessor(bi);
			if( stack == null) {
				ColorModel cm = slice.getColorModel();
				stack = new ImageStack(slice.getWidth(), slice.getHeight(), cm);
			}
			String tmp1 = "Compressed" + indx+ "\n" + orthSub.extractDicomInfo(uuid);
			stack.addSlice(tmp1, slice);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return stack;
	}

	void extractCalibration( ImagePlus img1, String meta) {
		double[] coeff = new double[2];
		float[] spacing;
		String tmp1;
		tmp1 = ChoosePetCt.getDicomValue(meta, "0028,1052");
		if( tmp1 == null) return;
		coeff[0] = ChoosePetCt.parseDouble(tmp1);
		tmp1 = ChoosePetCt.getDicomValue(meta, "0028,1053");
		if( tmp1 == null) return;
		coeff[1] = ChoosePetCt.parseDouble(tmp1);
		img1.getCalibration().setFunction(Calibration.STRAIGHT_LINE, coeff, "Gray Value");
		tmp1 = ChoosePetCt.getDicomValue(meta, "0028,0030");
		if( tmp1 == null) return;
		spacing = ChoosePetCt.parseMultFloat(tmp1);
		img1.getCalibration().pixelWidth = spacing[0];
		img1.getCalibration().pixelHeight = spacing[1];
		img1.getCalibration().setUnit("mm");
	}

	void deleteStudy(Object[] row1) {
		int i, n;
		String uuid1, tmp1, serName, serUID;
		orth1Study currStudy;
		JSONArray series;
		JSONObject ser1, main1;
		try {
			currStudy = getCurStudy(row1);
			if( currStudy == null) return;
			serName  = (String) row1[ReadBIdatabase.TBL_SERIES];
			serUID = ((String) row1[ReadBIdatabase.TBL_SER_UID]).trim();
			series = currStudy.seriesList;
			n = series.size();
			for( i=0; i<n; i++) {
				uuid1 = (String) series.get(i);
				ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid1);
				if( ser1 == null) continue;
				main1 = (JSONObject) ser1.get("MainDicomTags");
				tmp1 = (String) main1.get("SeriesDescription");
				if( tmp1 == null || tmp1.isEmpty()) tmp1 = "-";
				if(serName != null && !serName.equals(tmp1)) continue;
				tmp1 = ((String) main1.get("SeriesInstanceUID")).trim();
				if( !serUID.equals(tmp1)) continue;
				tmp1 = orthSub.baseURL + "series/" + uuid1;
				orthSub.runDelete(tmp1);
				break;
			}
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
	}

	orth1Study getCurStudy(Object[] row1) {
		int i, n = orthStudies.size();
		orth1Study currStudy = null;
		for( i=0; i<n; i++) {
			currStudy = orthStudies.get(i);
			if( isEqual(currStudy.patName, (String)row1[ReadBIdatabase.TBL_PAT_NAME])
				&& isEqual(currStudy.patID, (String)row1[ReadBIdatabase.TBL_PAT_ID])
				&& ChoosePetCt.isSameDate(currStudy.studyDate,(Date)row1[ReadBIdatabase.TBL_DATE])
				&& isEqual(currStudy.studyName, (String)row1[ReadBIdatabase.TBL_STUDY])) break;
		}
		if( i>= n) {
			parent.maybeRereadTable();
			JOptionPane.showMessageDialog(parent,"Orthanc study not found...\n"
				+ "Table refreshed. Maybe try again?");
			return null;
		}
		return currStudy;
	}
/*	String[] maybeAnnotations(String flName, boolean isConference) {
		File flOut, flIn = new File(flName);
		String gflName, par1;
		byte[] in1;
		FileInputStream flIn1;
		FileOutputStream flOut1;
		int i, k, n, sz1;
		try {
			DicomFormat dcm = new DicomFormat();
			par1 = flIn.getParent();
			k = dcm.checkDicomValid(par1, flIn.getName());
			if( k<=0) return null;
			flIn1 = new FileInputStream(flName);
			n = ((int) flIn.length()) - k;
			in1 = new byte[n];
			flIn1.skip(k);
			flIn1.read(in1);
			flIn1.close();
			for( i=0; i<n; i++) if( in1[i] == 0) break;
			if( i>=n) return null;
			sz1 = i;
			gflName = new String(in1, 0, sz1++, "ISO-8859-1");
			flOut = new File(par1 + File.separatorChar + gflName);
			if( !isConference) flOut.deleteOnExit();
			flOut1 = new FileOutputStream(flOut);
			flOut1.write(in1, sz1, n-sz1);
			flOut1.close();
			return ChoosePetCt.getFrameText(flOut.getPath());
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return null;
	}*/

	String[] getDuplicates(JSONArray series) {
		if( series == null || series.size() <= 1) return null;
		int i, j, k, n=series.size();
		Integer cnt;
		JSONObject ser1, main1;
		String[] retVal, curVal;
		String src, tst, uuid1;
		retVal = new String[n];
		curVal = new String[n];
		int[] dups = new int[n];
		for( i=0; i<n; i++) {
			uuid1 = (String) series.get(i);
			ser1 = (JSONObject) orthSub.ReadJson("series/" + uuid1);
			main1 = (JSONObject) ser1.get("MainDicomTags");
			tst = (String) main1.get("SeriesDescription");
			if(tst == null) tst = "";
			curVal[i] = tst;
			dups[i] = -1;
			retVal[i] = "";
		}
		for( i=0; i<n-1; i++) {
			src = curVal[i];
			for( j=i+1; j<n; j++) {
				tst = curVal[j];
				if( src.equals(tst) && dups[j]<0) dups[j]=i;
			}
		}
		for( i=0; i<n; i++) if(dups[i] >= 0) break;
		if( i>=n) return null;	// the usual case, no duplicates
		for( i=n-1; i>0; i--) {
			j = dups[i];	// the matching entry
			if( j<0) continue;
			cnt = 1;
			for( k=1; k<i; k++) if( j == dups[k]) cnt++;
			retVal[i] = cnt.toString();
		}
		return retVal;
	}

	BI_dbSaveInfo getBIsave(orth1Study curr1) {
		BI_dbSaveInfo currBI = new BI_dbSaveInfo();
		currBI.patName = ChoosePetCt.compressPatName(curr1.patName);
		currBI.patID = ChoosePetCt.compressID(curr1.patID);
		currBI.styDate = curr1.studyDate;
		currBI.styName = curr1.studyName;
		currBI.accession = curr1.accession;
		currBI.AETitle = parent.AETitle;
		currBI.basUrl = orthSub.baseURL;
		currBI.usrPW = orthSub.userPW;
		return currBI;
	}
}
