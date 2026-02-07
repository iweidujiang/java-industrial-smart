package io.github.iweidujiang.industry.indoor.twin.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 室内环境模型
 * <p>
 * 作者: 苏渡苇
 * GitHub:  https://github.com/iweidujiang
 * 公众号: 苏渡苇
 */
@Getter
public class IndoorEnvironment {

    /**
     * 区域信息
     */
    @Getter
    public static class Area {
        private final String name;
        private final List<double[]> boundaries; // 区域边界坐标点

        public Area(String name, List<double[]> boundaries) {
            this.name = name;
            this.boundaries = boundaries;
        }

        /**
         * 检查点是否在区域内
         * @param point 点坐标 [x, y]
         * @return 是否在区域内
         */
        public boolean containsPoint(double[] point) {
            int n = boundaries.size();
            if (n < 3) return false;

            boolean inside = false;
            double[] p1 = boundaries.get(0);

            for (int i = 1; i <= n; i++) {
                double[] p2 = boundaries.get(i % n);
                if (point[1] > Math.min(p1[1], p2[1])) {
                    if (point[1] <= Math.max(p1[1], p2[1])) {
                        if (point[0] <= Math.max(p1[0], p2[0])) {
                            if (p1[1] != p2[1]) {
                                double xInt = (point[1] - p1[1]) * (p2[0] - p1[0]) / (p2[1] - p1[1]) + p1[0];
                                if (p1[0] == p2[0] || point[0] <= xInt) {
                                    inside = !inside;
                                }
                            }
                        }
                    }
                }
                p1 = p2;
            }

            return inside;
        }
    }

    /**
     * -- GETTER --
     *  获取所有区域
     *
     * @return 区域列表
     */
    private final List<Area> areas;

    /**
     * 构造函数
     */
    public IndoorEnvironment() {
        this.areas = new ArrayList<>();
        initializeDefaultAreas();
    }

    /**
     * 初始化默认区域
     */
    private void initializeDefaultAreas() {
        // 区域1：会议室
        List<double[]> meetingRoomBoundaries = new ArrayList<>();
        meetingRoomBoundaries.add(new double[]{0, 0});
        meetingRoomBoundaries.add(new double[]{5, 0});
        meetingRoomBoundaries.add(new double[]{5, 5});
        meetingRoomBoundaries.add(new double[]{0, 5});
        areas.add(new Area("会议室", meetingRoomBoundaries));

        // 区域2：办公区
        List<double[]> officeAreaBoundaries = new ArrayList<>();
        officeAreaBoundaries.add(new double[]{5, 0});
        officeAreaBoundaries.add(new double[]{15, 0});
        officeAreaBoundaries.add(new double[]{15, 10});
        officeAreaBoundaries.add(new double[]{5, 10});
        areas.add(new Area("办公区", officeAreaBoundaries));

        // 区域3：休息区
        List<double[]> restAreaBoundaries = new ArrayList<>();
        restAreaBoundaries.add(new double[]{0, 5});
        restAreaBoundaries.add(new double[]{5, 5});
        restAreaBoundaries.add(new double[]{5, 10});
        restAreaBoundaries.add(new double[]{0, 10});
        areas.add(new Area("休息区", restAreaBoundaries));
    }

    /**
     * 根据坐标获取区域
     * @param position 位置坐标 [x, y]
     * @return 区域名称，未找到返回 "未知区域"
     */
    public String getAreaByPosition(double[] position) {
        for (Area area : areas) {
            if (area.containsPoint(position)) {
                return area.getName();
            }
        }
        return "未知区域";
    }

}
