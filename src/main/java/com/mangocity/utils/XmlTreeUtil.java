package com.mangocity.utils;

import java.util.List;

import com.mangocity.btms.core.model.hierarchy.HierarchyArchitecture;



public class XmlTreeUtil {


    public static String getModel(HierarchyArchitecture hierarchyArchitecture) {
        StringBuffer sb = new StringBuffer();
        getCostCenterList(hierarchyArchitecture, 1, sb, false, "");

        return sb.toString();

    }

    private static void getCostCenterList(HierarchyArchitecture hierarchyArchitecture, int i, StringBuffer sb, boolean endflag,String costName) {
        List costCenters = hierarchyArchitecture.getChildren();
        String retname = "";
        //节点取出属性值
        if(costName.length() > 0){
            retname = costName + "/" + hierarchyArchitecture.getName();
        } else{
            retname = hierarchyArchitecture.getName();
        }
        if (costCenters.size() == 0) {
            if (endflag) {
                sb.append("<li class=\"last\">");
            } else {
                sb.append("<li>");
            }
            sb.append("<input type=\"radio\"  name=\"costCenter\" value=\"" + retname + "\">" + hierarchyArchitecture.getName() + "</input></li>");
            sb.append("\n");
        } else {

            int ac = hierarchyArchitecture.getChildren().size();
            if (ac > 0 && hierarchyArchitecture.getParentId() != 0) {//剔除公司本身
                if(costName.length()>0){
                    costName +="/"+ hierarchyArchitecture.getName();
                } else{
                    costName = hierarchyArchitecture.getName();
                }

                sb.append("<li class=\"expandable\"><div class=\"hitarea expandable-hitarea\"></div>"
                      +  hierarchyArchitecture.getName() );
                sb.append("\n");
                sb.append("<ul style=\"display: none;\">");
                sb.append("\n");
                i++;
            }
            // 有子元素
            for (int in = 0; in < costCenters.size(); in++) {
                HierarchyArchitecture dep = (HierarchyArchitecture) costCenters.get(in);
                endflag = false;
                if (in == costCenters.size() - 1) {
                    endflag = true;
                }
                // 递归遍历
                getCostCenterList(dep, i, sb, endflag, costName);
            }
            if (ac > 0) {
                sb.append("</ul>");
                sb.append("\n");
                sb.append("</li>");
                sb.append("\n");
            }
        }
    }
}
