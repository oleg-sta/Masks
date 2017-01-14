//
// Created by admin on 01/01/2017.
//

#ifndef SHAPE3D_ORTHOGONALPROJECTIONMODEL_H
#define SHAPE3D_ORTHOGONALPROJECTIONMODEL_H

#include <dlib/matrix.h>
#include <unordered_map>
class OrthogonalProjectionModel
{
public:
    OrthogonalProjectionModel(int n_blendshapes);
    OrthogonalProjectionModel();
    dlib::matrix<double> get_initial_parameters(dlib::matrix<double> xx, dlib::matrix<double> yy);
    dlib::matrix<double> get_residuals(dlib::matrix<double> params,
                                       dlib::matrix<double> mean3d,
                                       std::unordered_map<int, dlib::matrix<double>> blendshapes,
                                       dlib::matrix<double> y) const;


    dlib::matrix<double> get_full_shape3d(dlib::matrix<double> params,
                                     dlib::matrix<double> mean3d,
                                     std::unordered_map<int, dlib::matrix<double>> blendshapes) const;

    dlib::matrix<double> convert_mean_shape(dlib::matrix<double> params,
                                     dlib::matrix<double> mean3d,
                                     std::unordered_map<int, dlib::matrix<double>> blendshapes) const;
private:
    int n_params = 6;
    const dlib::matrix<double>
    rogrigues(dlib::matrix<double> rotation_vector) const;
    int n_blendshapes;
    dlib::matrix<double> fun(dlib::matrix<double> x,
                             std::unordered_map<int, dlib::matrix<double> > blendshapes,
                             dlib::matrix<double> params) const;
};

#endif //SHAPE3D_ORTHOGONALPROJECTIONMODEL_H
