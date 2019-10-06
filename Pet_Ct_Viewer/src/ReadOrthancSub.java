import com.orthancserver.HttpsTrustModifier;
import ij.ImagePlus;
import ij.WindowManager;
import ij.io.FileInfo;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author ilan
 */
public class ReadOrthancSub {
	String baseURL = null, userPW = null;
	String AETitle = null;	// for annotations
	ImagePlus srcImg = null; // for annotations
	String currPatName=null, currSeriesUID=null;
	Object annot = null;

	ReadOrthancSub(String url, String upw) {
		baseURL = url;
		userPW = upw;
	}

	ReadOrthancSub(Object ann1) {
		annot = ann1;
	}

	ReadOrthancSub() {
	}

	Object ReadJson(String uri) {
		String content = "";
		int read;
		char[] chars;
		BufferedReader reader;
		try {
			InputStream stream = OpenUrl(baseURL + uri, null);
			if( stream == null) return null;
			reader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder buffer = new StringBuilder();
			chars = new char[16384];
			while((read = reader.read(chars)) != -1) buffer.append(chars, 0, read);
			reader.close();
			content = buffer.toString();
		} catch (Exception e) {ChoosePetCt.stackTrace2Log(e);}
		return JSONValue.parse(content);
	}

	InputStream OpenUrl(String urlString, String accept) {
		HttpURLConnection uc;
		InputStream ret1 = null;
		try {
			uc = OpenUrlSub( urlString, accept);
			if( uc == null) return null;
			ret1 =  uc.getInputStream();
		} catch(FileNotFoundException fnfe) {return null;}
		catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return ret1;
	}

	HttpURLConnection OpenUrlSub(String urlString, String accept) {
		HttpURLConnection uc = null;
		String tmp;
		try {
			URL url = new URL(urlString);
			uc = (HttpURLConnection) url.openConnection();
			HttpsTrustModifier.Trust(uc);
			if( userPW != null) {
				tmp = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userPW.getBytes());
				uc.setRequestProperty("Authorization", tmp);
			}
			if( accept != null) uc.setRequestProperty("Accept", accept);
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return uc;
	}

	boolean isOrthFile(int indx) {
		JFijiPipe pipIn;
		PetCtPanel parPan = null;
		BI_dbSaveInfo curDb;
		if( annot == null) return false;
		if( annot instanceof Annotations) parPan = ((Annotations) annot).parPanel;
		else if( annot instanceof AnoToolBar) parPan = ((AnoToolBar) annot).parPanel;
		else if( annot instanceof BrownFat) parPan = ((BrownFat) annot).getParentPet();
		if( parPan == null) return false;
		switch(indx) {
			default:
				pipIn = parPan.petPipe;
				break;

			case 1:
				pipIn = parPan.upetPipe;
				break;

			case 2:
				pipIn = parPan.ctPipe;
				break;
		}
		srcImg = pipIn.data1.srcImage;
		if( srcImg == null) return false;
		curDb = (BI_dbSaveInfo) srcImg.getProperty("bidb");
		if( curDb == null) return false;
		AETitle = curDb.AETitle;
		baseURL = curDb.basUrl;
		userPW = curDb.usrPW;
		return baseURL != null;	// only Orthanc has this
	}

	JSONArray findSeries(ImagePlus img1) {
		JSONArray patients, studies, series, empty;
		JSONObject patient=null, mainp, study=null, mains;
		String meta, patID, currName, currID, tmp1;
		String uuid0, uuid1, studyUID;
		int i, j;
		meta = ChoosePetCt.getMeta(1, img1);
		if( meta == null) return null;
		currPatName = ChoosePetCt.getCompressedPatName(meta);
		if( currPatName == null) return null;
		patID = ChoosePetCt.getCompressedID( meta);
		if( patID == null) return null;
		studyUID = ChoosePetCt.getDicomValue(meta, "0020,000D");
		if( studyUID == null) return null;
		currSeriesUID = ChoosePetCt.getDicomValue(meta, "0020,000E");
		if( currSeriesUID == null) return null;
		empty = new JSONArray();
		patients = (JSONArray) ReadJson("patients");
		for(i = 0; i < patients.size(); i++) {
			uuid0 = (String) patients.get(i);
			patient = (JSONObject) ReadJson("patients/" + uuid0);
			mainp = (JSONObject) patient.get("MainDicomTags");
			currName = ChoosePetCt.compressPatName( mainp.get("PatientName"));
			currID = ChoosePetCt.compressID( mainp.get("PatientID"));
			if( ChoosePetCt.are2StingsEqual(currName, currPatName) &&
				ChoosePetCt.are2StingsEqual(currID,patID)) break;
		}
		if( i >= patients.size() || patient == null) return empty;
		studies = (JSONArray) patient.get("Studies");
		for( j =0; j <  studies.size(); j++) {
			uuid1 = (String) studies.get(j);
			study = (JSONObject) ReadJson("studies/" + uuid1);
			mains = (JSONObject) study.get("MainDicomTags");
			tmp1 = (String) mains.get("StudyInstanceUID");
			if( studyUID.equals(tmp1)) break;
		}
		if( j >= studies.size() || study == null) return empty;
		series = (JSONArray) study.get("Series");
		return series;
	}

	void write2Orth( File fl1, int type) {
		JSONArray series;
		JSONObject ser1, main1;
		String tmp1, uuid2=null, curr3;
		int j, n;
		if( srcImg == null) return;
		series = findSeries(srcImg);
		if( series == null) return;
		// first delete previous annotations
		n = series.size();
		curr3 = currSeriesUID + ".3" + type;
		for( j=0; j<n; j++) {
			uuid2 = (String) series.get(j);
			ser1 = (JSONObject) ReadJson("series/" + uuid2);
			main1 = (JSONObject) ser1.get("MainDicomTags");
			tmp1 = (String) main1.get("SeriesInstanceUID");
			if( tmp1.equals(curr3)) break;
		}
		fl1.deleteOnExit();
		if( j<n) {	// do the erase
//			tmp1 = "curl -X DELETE " + baseURL + "series/" + uuid2;
//			if(userPW != null) tmp1 += " -u" +userPW;
//			runCurl(tmp1);
			tmp1 = baseURL + "series/" + uuid2;
			runDelete(tmp1);
		}
		myWriteDicom dcm1 = new myWriteDicom(srcImg);
		dcm1.writeRawData(fl1, AETitle, type);
	}

/*	void runCurl(String in) {
		try {
			Process myProc = Runtime.getRuntime().exec(in);
			if( myProc == null) return;
			myProc.waitFor();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}*/

	void runDelete(String in) {
		HttpURLConnection uc;
		int ret;
		try {
			uc = OpenUrlSub(in, null);
			if( uc == null) return;
			uc.setRequestProperty("Content-Type", "application/json");
			uc.setRequestMethod("DELETE");
			ret = uc.getResponseCode();
			uc.disconnect();
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e); }
	}

	void look4annotations(PetCtPanel pan1, int type) {
		int i, j, k, n, nAnno;
		ImagePlus ip, ip1;
		FileInfo fi;
		JFijiPipe pip1;
		String tmp1, meta, meta1, seriesUID, inPath, inName, outPath;
		String key1 = "Annotations";
		if( type == 1) key1 = "Brown fat";
		int[] annList, windowList = WindowManager.getIDList();
		n = windowList.length;
		annList = new int[n];
		for( i=nAnno=0; i<n; i++) {
			ip = WindowManager.getImage(windowList[i]);
			if( ip.getHeight()!= 1 || ip.getNSlices() != 1) continue;
			tmp1 = ip.getTitle();
			if( !tmp1.endsWith(key1)) continue;
			annList[nAnno++] = i;
		}
		for( i=0; i<nAnno; i++) {
			j = annList[i];
			ip = WindowManager.getImage(windowList[j]);
			fi = ip.getOriginalFileInfo();
			inPath = fi.directory;
			meta = ChoosePetCt.getMeta(1, ip);
			if( meta == null) continue;
			k = meta.indexOf('\n');
			inName = inPath + File.separator + meta.substring(0, k);
			seriesUID = ChoosePetCt.getDicomValue(meta, "0020,000E").trim();
			currSeriesUID = seriesUID.substring(0, seriesUID.length()-3);
			for( j=0; j<3; j++) {
				switch( j) {
					default:
						pip1 = pan1.petPipe;
						break;

					case 1:
						pip1 = pan1.ctPipe;
						break;

					case 2:
						pip1 = pan1.upetPipe;
						break;
				}
				if( pip1 == null) continue;
				meta1 = pip1.data1.metaData;
				if( meta1 == null) continue;
				seriesUID = ChoosePetCt.getDicomValue(meta1, "0020,000E").trim();
				if(seriesUID == null || !seriesUID.equals(currSeriesUID)) continue;
				ip1 = pip1.data1.srcImage;
				fi = ip1.getOriginalFileInfo();
				if( fi == null) continue;
				outPath = fi.directory;
				if( !maybeAnnotations(inName, outPath)) continue;
				if(type==1) pip1.data1.orthBF = ip;
				else pip1.data1.orthAnno = ip;
				break;
			}
		}
	}

	boolean maybeAnnotations(String flName, String outPath) {
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
			if( k<=0) return false;
			flIn1 = new FileInputStream(flName);
			n = ((int) flIn.length()) - k;
			in1 = new byte[n];
			flIn1.skip(k);
			flIn1.read(in1);
			flIn1.close();
			for( i=0; i<n; i++) if( in1[i] == 0) break;
			if( i>=n) return false;
			sz1 = i;
			gflName = new String(in1, 0, sz1++, "ISO-8859-1");
			flOut = new File(outPath + File.separatorChar + gflName);
			flOut.deleteOnExit(); // maybe not
			flOut1 = new FileOutputStream(flOut);
			flOut1.write(in1, sz1, n-sz1);
			flOut1.close();
			return true;
		} catch (Exception e) { ChoosePetCt.stackTrace2Log(e);}
		return false;
	}

	String extractDicomInfo(String uuid) {
		JSONObject tags = (JSONObject) ReadJson("instances/" + uuid + "/tags");
		if (tags == null || tags.isEmpty()) return "";
		String info = new String();
		String type1;

		ArrayList<String> tagsIndex = new ArrayList<>();
		for (Object tag : tags.keySet()) {
			tagsIndex.add((String) tag);
		}

		Collections.sort(tagsIndex);
		for (String tag : tagsIndex) {
			JSONObject value = (JSONObject) tags.get(tag);
			type1 = (String) value.get("Type");
			if (type1.equals("String")) {
				info += (tag + " " + (String) value.get("Name")
					+ ": " + (String) value.get("Value") + "\n");
			} else {
				if( type1.equals("Sequence")) {
					info = addSequence(info, value, tag);
				}
			}
		}
		return info;
	}

	private int seqDepth = 0;
	String addSequence(String info0, JSONObject value, Object tag) {
		String type2, info = info0;
		JSONArray seq0;
		JSONObject seqVal, vals;
		seq0 = (JSONArray)value.get("Value");
		if( seq0 == null || seq0.isEmpty()) {
			return info;	// ignore empty sequences
		}
		info += tag + getIndent() + (String) value.get("Name") +"\n";
		seqDepth++;
		seqVal = (JSONObject) seq0.get(0);

		ArrayList<String> tagsIndex = new ArrayList<>();
		for( Object tag0 : seqVal.keySet()) {
			tagsIndex.add((String) tag0);
		}
		Collections.sort(tagsIndex);

		for( Object tag1 : tagsIndex) {
			vals = (JSONObject) seqVal.get(tag1);
			type2 = (String) vals.get("Type");
			if( type2.equals("String")) {
				info += tag1 + getIndent() + (String) vals.get("Name")
					+ ": " + (String) vals.get("Value")+ "\n";
			} else {
				if(type2.equals("Sequence")) {
					info = addSequence(info, vals, tag1);
				}
			}
		}
		seqDepth--;
		return info;
	}
	
	String getIndent() {
		String indent = " ";
		for( int i=0; i<seqDepth; i++) indent += ">";
		return indent;
	}
}
