//
// Created by admin on 01/01/2017.
//

#include "3DFaceModel.h"
FaceModel3D::FaceModel3D(std::string path_to_models, int n_blendshapes)
{
    std::string meshPath = path_to_models + "//mesh.csv";
    std::string mean3DPath = path_to_models + "//meanShape3D.csv";
    std::string idxs3DPath = path_to_models + "//idx3d.csv";
    std::string idxs2DPath = path_to_models + "//idx2d.csv";

    std::string blendshapesPath = path_to_models + "//blendshapes//";
    std::streambuf *cinbuf = std::cin.rdbuf();
    std::ifstream shape3d(mean3DPath);
    std::cin.rdbuf(shape3d.rdbuf());
    std::cin >> FaceModel3D::mean3DShape;

    std::ifstream mesh(meshPath);
    std::cin.rdbuf(mesh.rdbuf());
    dlib::matrix<int> mesh_tmp;
    std::cin >> mesh_tmp;
    FaceModel3D::mesh = FaceModel3D::fix_mesh_winding(mesh_tmp, FaceModel3D::mean3DShape);


    std::ifstream idx3d(idxs3DPath);
    std::cin.rdbuf(idx3d.rdbuf());
    std::cin >> FaceModel3D::idxs3D;

    std::ifstream idx2d(idxs2DPath);
    std::cin.rdbuf(idx2d.rdbuf());
    std::cin >> FaceModel3D::idxs2D;

    for (int i = 0; i < n_blendshapes; ++i)
    {
        std::ostringstream oss;
        oss << blendshapesPath << "blendshape_" << i << ".csv";
        std::string current_blendshape = oss.str();
        std::ifstream blendshapes(current_blendshape);
        dlib::matrix<double> current_matrix;
        std::cin.rdbuf(blendshapes.rdbuf());
        std::cin >> current_matrix;
        FaceModel3D::blendshapes[i] = current_matrix;

    }
    std::cin.rdbuf(cinbuf);

        for(int n = 0; n < blendshapes.size(); n++)
        {
            //const dlib::matrix<double> blendshape = blendshapes[n];
            dlib::matrix<double> blendshape_return;
            blendshape_return.set_size(blendshapes.at(n).nr(), idxs3D.nr());
            for (int i = 0; i < idxs3D.nr(); ++i)
            {
                for (int j = 0; j < blendshapes.at(n).nr(); ++j)
                {
                    blendshape_return(j, i) = blendshapes.at(n)(j, idxs3D(i, 0));
                }
            }
            cur_blendshapes[n] = blendshape_return;
        }
            res.set_size(mean3DShape.nr(),idxs3D.nr());
            for (int i = 0; i < idxs3D.nr(); ++i)
            {
                for (int j = 0; j < mean3DShape.nr(); ++j)
                {
                    res(j, i) = mean3DShape(j, idxs3D(i, 0));
                }
            }
}
dlib::matrix<double>
FaceModel3D::fix_mesh_winding(const dlib::matrix<int> &mesh, const dlib::matrix<double> &vertices)
{
    dlib::matrix<double> meshOut;
    meshOut.set_size(mesh.nr(),mesh.nc());

    for (int i = 0; i < mesh.nr(); ++i)
    {
        dlib::matrix<int> triangle = dlib::rowm(mesh,i);
        dlib::matrix<double> cur_sub_mat = dlib::colm(vertices,triangle);
        dlib::matrix<double> normal = FaceModel3D::getNormal(cur_sub_mat);
        if(normal(2,0) > 0)
            dlib::set_rowm(meshOut,i) = FaceModel3D::flip_winding(triangle);
        else
            dlib::set_rowm(meshOut,i) = triangle;
    }
    return meshOut;
}
dlib::matrix<int>
FaceModel3D::flip_winding(dlib::matrix<int> triangle)
{
    dlib::matrix<int,1,3> out;
    out(0,0) = triangle(1,0);
    out(0,1) = triangle(0,0);
    out(0,2) = triangle(2,0);
    return out;
}
dlib::matrix<double>
FaceModel3D::getNormal(dlib::matrix<double> triangle)
{
    dlib::vector<double> a = dlib::colm(triangle,0);
    dlib::vector<double> b = dlib::colm(triangle,1);
    dlib::vector<double> c = dlib::colm(triangle,2);

    dlib::vector<double> axisX = b - a;
    axisX = dlib::normalize(axisX);
    dlib::vector<double> axisY = c - a;
    axisY = dlib::normalize(axisY);
    dlib::vector<double> x = axisX;
    dlib::vector<double> axisZ = axisX.cross(axisY);
    axisZ = dlib::normalize(axisZ);
    return axisZ;

}
dlib::matrix<int>
FaceModel3D::getIdxs2D()const
{
    return idxs2D;
}
/*
dlib::matrix<int> &
FaceModel3D::getIdxs3D() const
{
    return idxs3D;
}*/
dlib::matrix<double>
FaceModel3D::get_mean_shape3d() const
{
    return res;
}
std::unordered_map<int, dlib::matrix<double> > const
FaceModel3D::get_blendshapes() const
{
    return cur_blendshapes;
}
FaceModel3D::FaceModel3D() {}
std::unordered_map<int, dlib::matrix<double> > const
FaceModel3D::get_all_blendshapes() const
{
    return blendshapes;
}
dlib::matrix<double>
FaceModel3D::get_all_mean_shape3d() const
{
    return mean3DShape;
}

