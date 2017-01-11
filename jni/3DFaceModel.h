//
// Created by admin on 01/01/2017.
//

#ifndef SHAPE3D_SHAPEMODEL_H
#define SHAPE3D_SHAPEMODEL_H

#include <dlib/matrix.h>
#include <unordered_map>

class FaceModel3D
{
public:
    FaceModel3D(std::string models_path, int n);
    FaceModel3D();
    dlib::matrix<double> get_mean_shape3d() const;
    dlib::matrix<double> get_all_mean_shape3d() const;

    dlib::matrix<int>
    getIdxs2D() const;

/*
    dlib::matrix<int> &
    getIdxs3D() const;
*/

    std::unordered_map<int, dlib::matrix<double> > const
    get_blendshapes() const;

    std::unordered_map<int, dlib::matrix<double> > const
    get_all_blendshapes() const;

private:
    dlib::matrix<double> mean3DShape;
    dlib::matrix<int> idxs2D;
    dlib::matrix<double>
    fix_mesh_winding(const dlib::matrix<int> &matrix,const dlib::matrix<double> &vertices);
    dlib::matrix<int>  flip_winding(dlib::matrix<int> triangle);
    dlib::matrix<double>
    getNormal(dlib::matrix<double> matrix);
    std::unordered_map<int, dlib::matrix<double> > blendshapes ;

    dlib::matrix<double> mesh;
    dlib::matrix<int> idxs3D;
};

#endif //SHAPE3D_SHAPEMODEL_H
