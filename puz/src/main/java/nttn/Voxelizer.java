package nttn;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

class Voxels {
    // TODO: this will soon be SVOs (Spare Voxel Octrees) for more efficient voxel storage 
    // -> changing the data structures -> lead to: change voxelization algorithm, change rendering algorithm,...
    
    public int[][][] value;
    Voxels(int[][][] value) {
        this.value = value;
    }

    @Override 
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int zLen = value[0][0].length;  // Length along the z-axis

        for (int z = 0; z < zLen; z++) {
            sb.append("z = ").append(z).append("\n");
            for (int x = 0; x < value.length; x++) {
                for (int y = 0; y < value[x].length; y++) {
                    sb.append(value[x][y][z]).append(" ");
                }
                sb.append("\n");
            }
            sb.append("\n");  // Separate slices with an extra newline
        }

        return sb.toString();
    }

    // Method to serialize to JSON
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Method to serialize to JSON
    public void toJson(String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Voxelizer{
    public static Voxels voxelizeSphere(int resolution) {
        int[][][] value = new int[resolution][resolution][resolution];
        double radius = resolution / 2.0;
        double center = (resolution - 1) / 2.0;

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    // Calculate distance from the center
                    double distance = Math.sqrt(
                        Math.pow(x - center, 2) + 
                        Math.pow(y - center, 2) + 
                        Math.pow(z - center, 2)
                    );

                    // If the distance is less than or equal to the radius, it's inside the sphere
                    if (distance <= radius) {
                        value[x][y][z] = 1;
                    } else {
                        value[x][y][z] = 0;
                    }
                }
            }
        }
        return new Voxels(value);
    }


    public static Voxels voxelizeCone(int resolution) {
        int[][][] value = new int[resolution][resolution][resolution];
        double height = resolution;
        double radius = resolution / 2.0;
        double centerX = (resolution - 1) / 2.0;
        double centerY = (resolution - 1) / 2.0;

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    // Calculate the distance from the center and height ratio
                    double distanceFromCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));
                    double heightRatio = (height - z) / height;

                    // Check if the distance is within the cone's radius at this height
                    if (distanceFromCenter <= radius * heightRatio) {
                        value[x][y][z] = 1;
                    } else {
                        value[x][y][z] = 0;
                    }
                }
            }
        }

        return new Voxels(value);
    }


    public static Voxels voxelizeCylinder(int resolution) {
        int[][][] value = new int[resolution][resolution][resolution];
        double radius = resolution / 4.0; // Adjust the radius as needed
        double centerX = (resolution - 1) / 2.0;
        double centerY = (resolution - 1) / 2.0;

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    // Calculate the distance from the center
                    double distanceFromCenter = Math.sqrt(Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2));

                    // Check if the distance is within the cylinder's radius
                    if (distanceFromCenter <= radius && z < resolution) {
                        value[x][y][z] = 1;
                    } else {
                        value[x][y][z] = 0;
                    }
                }
            }
        }

        return new Voxels(value);
    }

    public static Voxels voxelizeTorus(int resolution) {
        int[][][] value = new int[resolution][resolution][resolution];
        double R = resolution / 4.0; // Distance from the center of the tube to the center of the torus
        double r = resolution / 8.0; // Radius of the tube

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                for (int z = 0; z < resolution; z++) {
                    // Translate coordinates to center the torus
                    double xCentered = x - (resolution / 2.0);
                    double yCentered = y - (resolution / 2.0);
                    double zCentered = z - (resolution / 2.0);

                    // Calculate the distance in the xy-plane
                    double distanceXY = Math.sqrt(xCentered * xCentered + yCentered * yCentered);
                    // Calculate the distance from the center of the tube
                    double distanceFromTubeCenter = Math.sqrt((distanceXY - R) * (distanceXY - R) + zCentered * zCentered);

                    // Check if the point is within the torus
                    if (distanceFromTubeCenter <= r) {
                        value[x][y][z] = 1;
                    } else {
                        value[x][y][z] = 0;
                    }
                }
            }
        }

        return new Voxels(value);
    }

    public static Voxels voxelizeTriangle(float[][] vertices, int resolution) {
        // Calculate the bounding box of the triangle
        float minX = Math.min(Math.min(vertices[0][0], vertices[1][0]), vertices[2][0]);
        float maxX = Math.max(Math.max(vertices[0][0], vertices[1][0]), vertices[2][0]);
        float minY = Math.min(Math.min(vertices[0][1], vertices[1][1]), vertices[2][1]);
        float maxY = Math.max(Math.max(vertices[0][1], vertices[1][1]), vertices[2][1]);
        float minZ = Math.min(Math.min(vertices[0][2], vertices[1][2]), vertices[2][2]);
        float maxZ = Math.max(Math.max(vertices[0][2], vertices[1][2]), vertices[2][2]);

        // Calculate voxel grid size based on the resolution
        int gridX = (int)((maxX - minX) * resolution);
        int gridY = (int)((maxY - minY) * resolution);
        int gridZ = (int)((maxZ - minZ) * resolution);

        int[][][] value = new int[gridX][gridY][gridZ];

        // Iterate through the bounding box
        for (int x = 0; x < gridX; x++) {
            for (int y = 0; y < gridY; y++) {
                for (int z = 0; z < gridZ; z++) {
                    // Calculate the center of the current voxel
                    float voxelX = minX + (x + 0.5f) / resolution;
                    float voxelY = minY + (y + 0.5f) / resolution;
                    float voxelZ = minZ + (z + 0.5f) / resolution;

                    // Check if the voxel is inside the triangle
                    if (isPointInTriangle(voxelX, voxelY, voxelZ, vertices)) {
                        value[x][y][z] = 1; // Fill the voxel
                    }
                }
            }
        }

        return new Voxels(value);
    }
    //
    // public static Voxels voxelizeObjModel(String filePath, int resolution) {
    //     List<float[]> vertices = new ArrayList<>();
    //     List<int[]> faces = new ArrayList<>();
    //
    //     // Load the OBJ file
    //     try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
    //         String line;
    //         while ((line = br.readLine()) != null) {
    //             String[] tokens = line.split(" ");
    //             if (tokens[0].equals("v")) {
    //                 // Parse vertex coordinates
    //                 float[] vertex = new float[3];
    //                 vertex[0] = Float.parseFloat(tokens[1]);
    //                 vertex[1] = Float.parseFloat(tokens[2]);
    //                 vertex[2] = Float.parseFloat(tokens[3]);
    //                 vertices.add(vertex);
    //             } else if (tokens[0].equals("f")) {
    //                 // Parse face indices (OBJ format is 1-indexed)
    //                 int[] face = new int[tokens.length - 1];
    //                 for (int i = 1; i < tokens.length; i++) {
    //                     face[i - 1] = Integer.parseInt(tokens[i].split("/")[0]) - 1; // Convert to 0-indexed
    //                 }
    //                 faces.add(face);
    //             }
    //         }
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    //
    //     // Convert lists to arrays
    //     float[][] vertexArray = vertices.toArray(new float[0][]);
    //     int[][] faceArray = faces.toArray(new int[0][]);
    //
    //     // Voxelization process using the existing voxelizeTriangle method
    //     for (int[] face : faceArray) {
    //         // Get the vertices of the face
    //         float[][] faceVertices = new float[face.length][3];
    //         for (int i = 0; i < face.length; i++) {
    //             faceVertices[i] = vertexArray[face[i]];
    //         }
    //
    //         // Assuming that each face is a triangle, voxelize the triangle
    //         progammvoxelizeTriangle(faceVertices, resolution);
    //     }
    //
    //     return new Voxels({});
    // }




    // Helper method to determine if a point is inside the triangle
    private static boolean isPointInTriangle(float px, float py, float pz, float[][] vertices) {
        // Use barycentric coordinates or another method to check if the point is inside the triangle
        // For simplicity, we'll just check if the point is close to the triangle's plane
        // (a more robust implementation is recommended)

        // Calculate the vectors
        float[] v0 = {vertices[1][0] - vertices[0][0], vertices[1][1] - vertices[0][1], vertices[1][2] - vertices[0][2]};
        float[] v1 = {vertices[2][0] - vertices[0][0], vertices[2][1] - vertices[0][1], vertices[2][2] - vertices[0][2]};
        float[] v2 = {px - vertices[0][0], py - vertices[0][1], pz - vertices[0][2]};

        // Compute dot products
        float dot00 = dot(v0, v0);
        float dot01 = dot(v0, v1);
        float dot02 = dot(v0, v2);
        float dot11 = dot(v1, v1);
        float dot12 = dot(v1, v2);

        // Compute barycentric coordinates
        float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Check if the point is inside the triangle
        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    // Dot product helper function
    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }


    public static void main(String[] args) {
        //float[][] vertices = {{10, 20, 30}, {40, 50, 60}, {70, 80, 90}};
        Voxels voxelTorus = Voxelizer.voxelizeCone(20);
        //System.out.println(voxelSphere);
        voxelTorus.toJson("./cone.json");
    }
}

